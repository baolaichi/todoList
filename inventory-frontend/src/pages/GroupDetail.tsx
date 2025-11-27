import React, { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { 
  Row, Col, Card, List, Input, Button, Avatar, Tag, Tabs, 
  message, Tooltip, Modal, Form, Table, Select, DatePicker, Empty, Descriptions, Popconfirm 
} from 'antd';
import { 
  SendOutlined, 
  UserAddOutlined, 
  PlusOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import axiosClient from '../api/axiosClient';
import type { ChatMessage, GroupMember, Task } from '../types';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import dayjs from 'dayjs';

const GroupDetail: React.FC = () => {
  const { id } = useParams(); 
  const myUsername = localStorage.getItem('username');
  
  // --- 1. FIX WARNING ANTD: Dùng hook useMessage ---
  const [messageApi, contextHolder] = message.useMessage();

  // --- STATE ---
  const [groupInfo, setGroupInfo] = useState<any>(null);
  
  // Chat
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  
  // Members
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [isAddMemberOpen, setIsAddMemberOpen] = useState(false);

  // Task
  const [groupTasks, setGroupTasks] = useState<Task[]>([]);
  const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null); // Lưu task đang sửa
  const [taskForm] = Form.useForm();

  // Xem chi tiết
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [viewTask, setViewTask] = useState<Task | null>(null);

  // Refs
  const stompClientRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // --- HELPER FUNCTIONS (PHÂN QUYỀN) ---
  // Lấy ID của bản thân trong list member
  const getMyUserId = () => {
      const me = members.find(m => m.username === myUsername);
      return me ? me.userId : -1;
  };

  const isLeader = () => groupInfo?.myRole === 'LEADER';

  // Logic: Leader HOẶC Người được giao task thì được sửa
  const canEditTask = (task: Task) => {
      return isLeader() || task.userId === getMyUserId();
  };

  const addMessageToState = (newMessage: ChatMessage) => {
    setMessages((prev) => {
        const exists = prev.some(m => m.id === newMessage.id);
        if (exists) return prev;
        return [...prev, newMessage];
    });
  };

  // --- INIT DATA ---
  useEffect(() => {
    if (!id) return;

    axiosClient.get(`/user/groups/${id}`).then(res => setGroupInfo(res.data)).catch(() => {});
    loadMembers();
    loadGroupTasks();
    
    axiosClient.get(`/chat/history/${id}`).then(res => {
      setMessages(res.data);
      setTimeout(scrollToBottom, 100);
    });

    const socket = new SockJS('http://localhost:8080/ws');
    const client = Stomp.over(socket);
    client.debug = null; 
    const token = localStorage.getItem('token');
    
    client.connect({ 'Authorization': `Bearer ${token}` }, () => {
      client.subscribe(`/topic/group/${id}`, (payload) => {
        addMessageToState(JSON.parse(payload.body));
        setTimeout(scrollToBottom, 100);
      });
    }, () => {});

    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current?.connected) stompClientRef.current.disconnect();
    };
  }, [id]);

  useEffect(() => scrollToBottom(), [messages]);
  const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });

  const loadMembers = () => axiosClient.get(`/user/groups/${id}/members`).then(res => setMembers(res.data));
  const loadGroupTasks = () => axiosClient.get(`/user/groups/${id}/tasks`).then(res => setGroupTasks(res.data));

  // --- CHAT ---
  const handleSendChat = async () => {
    if (!inputMsg.trim()) return;
    const tempMsg = inputMsg;
    setInputMsg('');
    try {
      const res = await axiosClient.post('/chat/send', { groupId: Number(id), content: tempMsg });
      addMessageToState(res.data);
      setTimeout(scrollToBottom, 50);
    } catch (error) {
      messageApi.error("Lỗi gửi tin nhắn!");
      setInputMsg(tempMsg);
    }
  };

  // --- MEMBER ---
  const handleAddMember = async (values: any) => {
    try {
      await axiosClient.post(`/user/groups/${id}/members`, { emailOrUsername: values.username });
      messageApi.success("Thêm thành viên thành công!");
      setIsAddMemberOpen(false);
      loadMembers();
    } catch (error: any) {
      messageApi.error(error.response?.data || "Lỗi thêm thành viên");
    }
  };

  // --- TASK MANAGEMENT (CRUD) ---

  // 1. Mở Modal Giao việc (Tạo mới)
  const openCreateTaskModal = () => {
      setEditingTask(null);
      taskForm.resetFields();
      setIsTaskModalOpen(true);
  };

  // 2. Mở Modal Sửa việc
  const openEditTaskModal = (task: Task) => {
      setEditingTask(task);
      taskForm.setFieldsValue({
          title: task.title,
          description: task.description,
          assignToUserId: task.userId,
          priority: task.priority,
          status: task.status,
          deadline: task.deadline ? dayjs(task.deadline) : null
      });
      setIsTaskModalOpen(true);
  };

  // 3. Xử lý Lưu (Tạo hoặc Sửa)
  const handleSaveTask = async (values: any) => {
    try {
      const payload = {
        ...values,
        groupId: Number(id),
        deadline: values.deadline ? values.deadline.format('YYYY-MM-DDTHH:mm:ss') : null,
      };
      
      if (editingTask) {
          // --- LOGIC SỬA TASK ---
          await axiosClient.put(`/user/groups/tasks/${editingTask.id}`, payload);
          messageApi.success("Cập nhật công việc thành công!");
      } else {
          // --- LOGIC TẠO TASK ---
          await axiosClient.post('/user/groups/tasks', payload);
          messageApi.success("Đã giao việc thành công!");
      }
      
      setIsTaskModalOpen(false);
      taskForm.resetFields();
      loadGroupTasks();

      // Nếu đang mở chi tiết thì đóng lại hoặc reload chi tiết
      if(isDetailOpen) setIsDetailOpen(false); 

    } catch (error: any) {
      messageApi.error(error.response?.data || "Lỗi xử lý công việc!");
    }
  };

  // 4. Xử lý Xóa Task (Chỉ Leader)
  const handleDeleteTask = async (taskId: number) => {
      try {
          await axiosClient.delete(`/user/groups/tasks/${taskId}`);
          messageApi.success("Đã xóa công việc!");
          loadGroupTasks();
      } catch (error: any) {
          messageApi.error(error.response?.data || "Lỗi xóa công việc");
      }
  };

  // 5. Cập nhật nhanh trạng thái (Nút check)
  const handleUpdateStatus = async (taskId: number, newStatus: string) => {
      if (!taskId) {
          messageApi.error("Lỗi ID Task = 0. Kiểm tra Backend!");
          return;
      }
      try {
          await axiosClient.patch(`/user/tasks/${taskId}/status`, { status: newStatus });
          messageApi.success("Cập nhật trạng thái thành công!");
          
          loadGroupTasks(); // Reload bảng
          
          // Nếu đang mở modal chi tiết thì cập nhật luôn state modal
          if (viewTask && viewTask.id === taskId) {
              setViewTask({ ...viewTask, status: newStatus as any });
          }
      } catch (error: any) {
          messageApi.error(error.response?.data || "Lỗi cập nhật trạng thái");
      }
  };

  // 6. Xem chi tiết
  const openDetailModal = async (taskId: number) => {
    try {
        const res = await axiosClient.get(`/user/tasks/detail/${taskId}`);
        setViewTask(res.data);
        setIsDetailOpen(true);
    } catch (error) {
        messageApi.error("Không thể xem chi tiết task này");
    }
  };

  // --- TABLE COLUMNS ---
  const taskColumns = [
    { title: 'Công việc', dataIndex: 'title', render: (t: string) => <b>{t}</b> },
    { title: 'Người làm', dataIndex: 'userId', render: (uid: number) => {
        const user = members.find(m => m.userId === uid);
        return user ? <Tag color="blue">{user.username}</Tag> : <Tag>N/A</Tag>;
    }},
    { title: 'Hạn chót', dataIndex: 'deadline', width: 100, render: (d: string) => d ? dayjs(d).format('DD/MM') : '-' },
    { title: 'Trạng thái', dataIndex: 'status', width: 90, render: (s: string) => <Tag color={s === 'DONE' ? 'green' : 'orange'}>{s}</Tag> },
    {
        title: '',
        key: 'action',
        width: 120,
        render: (_: any, record: Task) => (
            <div style={{display: 'flex', gap: 5}}>
                {/* Nút Xem */}
                <Tooltip title="Xem">
                    <Button size="small" icon={<EyeOutlined />} onClick={() => openDetailModal(record.id)} />
                </Tooltip>

                {/* Nút Sửa: Hiện nếu là Leader HOẶC là Task của mình */}
                {canEditTask(record) && (
                    <Tooltip title="Sửa">
                         <Button size="small" type="text" icon={<EditOutlined style={{color: '#1890ff'}}/>} onClick={() => openEditTaskModal(record)} />
                    </Tooltip>
                )}

                {/* Nút Xóa: Chỉ hiện nếu là Leader */}
                {isLeader() && (
                    <Popconfirm title="Xóa task này?" onConfirm={() => handleDeleteTask(record.id)}>
                        <Button size="small" type="text" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                )}
            </div>
        )
    }
  ];

  return (
    <div style={{ height: '85vh', display: 'flex', flexDirection: 'column' }}>
      {contextHolder} {/* Placeholder cho Message Antd */}

      <Card style={{ marginBottom: 16, borderRadius: 12 }} styles={{ body: { padding: '12px 24px' } }}>
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
            <div>
                <h2 style={{margin: 0, color: '#1890ff'}}>{groupInfo?.name || 'Đang tải...'}</h2>
                <span style={{color: '#888'}}>{groupInfo?.description}</span>
            </div>
            <Avatar.Group maxCount={5}>
                {members.map(m => (
                    <Tooltip title={`${m.username} (${m.role})`} key={m.userId}>
                        <Avatar style={{backgroundColor: m.role === 'LEADER' ? '#f56a00' : '#87d068'}}>{m.username[0].toUpperCase()}</Avatar>
                    </Tooltip>
                ))}
            </Avatar.Group>
          </div>
      </Card>

      <Row gutter={16} style={{ flex: 1, overflow: 'hidden' }}>
        {/* CHAT */}
        <Col span={14} style={{ height: '100%' }}>
          <Card title="Thảo luận nhóm" style={{ height: '100%', display: 'flex', flexDirection: 'column', borderRadius: 12 }} styles={{ body: { flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', padding: 16 } }}>
            <div style={{ flex: 1, overflowY: 'auto', paddingRight: 10, marginBottom: 10 }}>
              {messages.length === 0 && <Empty description="Chưa có tin nhắn nào" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                {messages.map((msg, index) => {
                  const isMe = msg.sender.username === myUsername;
                  return (
                    <div key={index} style={{ textAlign: isMe ? 'right' : 'left', marginBottom: 16 }}>
                      <div style={{ fontSize: 11, color: '#999', marginBottom: 2, padding: '0 4px' }}>
                        {!isMe && <strong>{msg.sender.username} • </strong>} 
                        {dayjs(msg.sentAt).format('HH:mm')}
                      </div>
                      <span style={{ display: 'inline-block', padding: '10px 16px', borderRadius: 20, background: isMe ? 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)' : '#f0f2f5', color: isMe ? '#fff' : '#333', maxWidth: '80%', wordBreak: 'break-word', textAlign: 'left' }}>
                        {msg.content}
                      </span>
                    </div>
                  );
                })}
              </div>
              <div ref={messagesEndRef} />
            </div>
            <div style={{ display: 'flex', gap: 10, paddingTop: 10, borderTop: '1px solid #f0f0f0' }}>
              <Input value={inputMsg} onChange={e => setInputMsg(e.target.value)} onPressEnter={handleSendChat} placeholder="Nhập tin nhắn..." style={{borderRadius: 20}} />
              <Button type="primary" shape="circle" icon={<SendOutlined />} onClick={handleSendChat} />
            </div>
          </Card>
        </Col>

        {/* TASK & MEMBER */}
        <Col span={10} style={{ height: '100%' }}>
            <Card style={{ height: '100%', borderRadius: 12 }} styles={{ body: { padding: 0, height: '100%' } }}>
                <Tabs defaultActiveKey="1" type="card" style={{height: '100%'}} 
                    items={[
                        {
                            key: '1',
                            label: 'Công việc',
                            children: (
                                <div style={{padding: 12, height: '100%', overflowY: 'auto'}}>
                                    {isLeader() && (
                                        <Button type="primary" block icon={<PlusOutlined />} onClick={openCreateTaskModal} style={{marginBottom: 10}}>
                                            Giao việc mới
                                        </Button>
                                    )}
                                    <Table dataSource={groupTasks} columns={taskColumns} rowKey="id" pagination={false} size="small" scroll={{ y: 400 }} />
                                </div>
                            )
                        },
                        {
                            key: '2',
                            label: `Thành viên (${members.length})`,
                            children: (
                                <div style={{padding: 12}}>
                                    {isLeader() && (
                                        <Button type="dashed" block icon={<UserAddOutlined />} onClick={() => setIsAddMemberOpen(true)} style={{marginBottom: 16}}>
                                            Mời thành viên
                                        </Button>
                                    )}
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                                      {members.map((item) => (
                                        <div key={item.userId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                                                <Avatar style={{backgroundColor: '#87d068'}}>{item.username[0].toUpperCase()}</Avatar>
                                                <div><div style={{ fontWeight: 500 }}>{item.username}</div><div style={{ fontSize: 11, color: '#999' }}>{item.email}</div></div>
                                            </div>
                                            <Tag color={item.role === 'LEADER' ? 'gold' : 'default'}>{item.role}</Tag>
                                        </div>
                                      ))}
                                    </div>
                                </div>
                            )
                        }
                    ]}
                />
            </Card>
        </Col>
      </Row>

      {/* MODAL MỜI THÀNH VIÊN */}
      <Modal title="Mời thành viên vào nhóm" open={isAddMemberOpen} onCancel={() => setIsAddMemberOpen(false)} footer={null}>
          <Form onFinish={handleAddMember} layout="vertical">
              <Form.Item name="username" label="Email hoặc Tên đăng nhập" rules={[{required: true}]}>
                  <Input placeholder="Nhập email/username..." />
              </Form.Item>
              <Button type="primary" htmlType="submit" block>Mời ngay</Button>
          </Form>
      </Modal>

      {/* MODAL GIAO VIỆC / SỬA VIỆC */}
      <Modal title={editingTask ? "Cập nhật công việc" : "Giao việc mới"} open={isTaskModalOpen} onCancel={() => setIsTaskModalOpen(false)} footer={null}>
          <Form form={taskForm} onFinish={handleSaveTask} layout="vertical">
              <Form.Item name="title" label="Tiêu đề công việc" rules={[{required: true}]}><Input /></Form.Item>
              <Form.Item name="description" label="Mô tả"><Input.TextArea rows={2} /></Form.Item>
              
              {/* Chỉ Leader mới được đổi người, Member chỉ xem mình được giao */}
              <Form.Item name="assignToUserId" label="Giao cho" rules={[{required: true}]}>
                  <Select placeholder="Chọn thành viên" disabled={!isLeader()}>
                      {members.map(m => (
                          <Select.Option key={m.userId} value={m.userId}>{m.username} ({m.email})</Select.Option>
                      ))}
                  </Select>
              </Form.Item>

              <div style={{ display: 'flex', gap: 10 }}>
                  <Form.Item name="deadline" label="Hạn chót" style={{flex:1}}><DatePicker showTime style={{width: '100%'}} /></Form.Item>
                  <Form.Item name="priority" label="Độ ưu tiên" style={{flex:1}} initialValue="MEDIUM">
                      <Select><Select.Option value="HIGH">Cao</Select.Option><Select.Option value="MEDIUM">TB</Select.Option><Select.Option value="LOW">Thấp</Select.Option></Select>
                  </Form.Item>
              </div>

              {/* Cho phép sửa Status trong form nếu là Edit */}
              {editingTask && (
                  <Form.Item name="status" label="Trạng thái">
                      <Select>
                          <Select.Option value="TODO">Chưa làm</Select.Option>
                          <Select.Option value="IN_PROGRESS">Đang làm</Select.Option>
                          <Select.Option value="DONE">Hoàn thành</Select.Option>
                      </Select>
                  </Form.Item>
              )}

              <Button type="primary" htmlType="submit" block>{editingTask ? "Lưu thay đổi" : "Giao việc"}</Button>
          </Form>
      </Modal>

      {/* MODAL CHI TIẾT & TRẠNG THÁI NHANH */}
      <Modal title="Chi tiết công việc" open={isDetailOpen} onCancel={() => setIsDetailOpen(false)} footer={[<Button key="close" onClick={() => setIsDetailOpen(false)}>Đóng</Button>]}>
        {viewTask ? (
          <div>
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="Tiêu đề"><b style={{fontSize: 16}}>{viewTask.title}</b></Descriptions.Item>
                <Descriptions.Item label="Mô tả" style={{whiteSpace: 'pre-wrap'}}>{viewTask.description || 'Không có mô tả'}</Descriptions.Item>
                <Descriptions.Item label="Trạng thái"><Tag color={viewTask.status === 'DONE' ? 'green' : viewTask.status === 'IN_PROGRESS' ? 'orange' : 'default'}>{viewTask.status}</Tag></Descriptions.Item>
                <Descriptions.Item label="Hạn chót">{viewTask.deadline ? dayjs(viewTask.deadline).format('HH:mm DD/MM') : '-'}</Descriptions.Item>
                <Descriptions.Item label="Người làm">{members.find(m => m.userId === viewTask.userId)?.username}</Descriptions.Item>
              </Descriptions>

              {/* Nút cập nhật trạng thái nhanh (Dành cho người được giao hoặc Leader) */}
              {canEditTask(viewTask) && (
                  <div style={{marginTop: 20, paddingTop: 15, borderTop: '1px dashed #ccc', display: 'flex', gap: 10, justifyContent: 'center'}}>
                      <Button onClick={() => handleUpdateStatus(viewTask.id, 'IN_PROGRESS')}>Đang làm</Button>
                      <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => handleUpdateStatus(viewTask.id, 'DONE')}>Hoàn thành</Button>
                  </div>
              )}
          </div>
        ) : <p>Đang tải...</p>}
      </Modal>
    </div>
  );
};

export default GroupDetail;