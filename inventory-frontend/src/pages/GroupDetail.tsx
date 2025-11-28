import React, { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { 
  Row, Col, Card, Input, Button, Avatar, Tag, Tabs, 
  message, Tooltip, Modal, Form, Table, Select, DatePicker, Empty, Descriptions, Popconfirm, Upload, Breadcrumb 
} from 'antd';
import { 
  SendOutlined, UserAddOutlined, PlusOutlined, EyeOutlined, CheckCircleOutlined, 
  EditOutlined, DeleteOutlined, FolderOutlined, FileTextOutlined, UploadOutlined, 
  HomeOutlined, ArrowUpOutlined, FileImageOutlined, FilePdfOutlined, DownloadOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import axiosClient from '../api/axiosClient';
import type { ChatMessage, GroupMember, Task } from '../types';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import dayjs from 'dayjs';

// URL Backend để xem file
const API_BASE_URL = 'http://localhost:8080';

const GroupDetail: React.FC = () => {
  const { id } = useParams(); 
  const myUsername = localStorage.getItem('username');
  const [messageApi, contextHolder] = message.useMessage();
  
  // --- STATE DỮ LIỆU CHUNG ---
  const [groupInfo, setGroupInfo] = useState<any>(null);
  const [members, setMembers] = useState<GroupMember[]>([]);
  
  // --- STATE CHAT ---
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const stompClientRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // --- STATE TASK (CÔNG VIỆC) ---
  const [groupTasks, setGroupTasks] = useState<Task[]>([]);
  const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [taskForm] = Form.useForm();
  
  // --- STATE CHI TIẾT TASK ---
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [viewTask, setViewTask] = useState<Task | null>(null);

  // --- STATE TÀI LIỆU (FOLDER/FILE) ---
  const [currentFolderId, setCurrentFolderId] = useState<number | null>(null);
  const [folderContent, setFolderContent] = useState<any>({ subFolders: [], files: [] });
  const [isCreateFolderOpen, setIsCreateFolderOpen] = useState(false);
  const [folderForm] = Form.useForm();
  
  // State Preview File (Ảnh/PDF)
  const [previewFile, setPreviewFile] = useState<{ url: string, type: string, name: string } | null>(null);

  // --- STATE MEMBER ---
  const [isAddMemberOpen, setIsAddMemberOpen] = useState(false);

  // --- HELPER CHECK QUYỀN ---
  const getMyUserId = () => members.find(m => m.username === myUsername)?.userId;
  const isLeader = () => groupInfo?.myRole === 'LEADER';
  
  const canEditTask = (task: Task) => isLeader() || task.userId === getMyUserId();
  const canDeleteTask = () => isLeader();

  // --- 1. INIT DATA (KHỞI TẠO DỮ LIỆU) ---
  useEffect(() => {
    if (!id) return;

    // Load data tĩnh
    axiosClient.get(`/user/groups/${id}`).then(res => setGroupInfo(res.data)).catch(() => {});
    loadMembers();
    loadGroupTasks();
    loadFolderContent(null); // Load thư mục gốc
    
    // Load Chat History
    axiosClient.get(`/chat/history/${id}`).then(res => {
      setMessages(res.data);
      setTimeout(scrollToBottom, 100);
    });

    // WebSocket Connection
    const socket = new SockJS('http://localhost:8080/ws');
    const client = Stomp.over(socket);
    client.debug = null; 
    const token = localStorage.getItem('token');
    
    client.connect({ 'Authorization': `Bearer ${token}` }, () => {
      client.subscribe(`/topic/group/${id}`, (payload) => {
        const newMessage = JSON.parse(payload.body);
        // Check trùng lặp trước khi thêm (vì có thể mình đã add optimistic rồi)
        setMessages((prev) => {
            const exists = prev.some(m => m.id === newMessage.id);
            if (exists) return prev;
            return [...prev, newMessage];
        });
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
  
  // --- LOGIC TÀI LIỆU (FOLDER) ---
  const loadFolderContent = async (folderId: number | null) => {
      try {
          const res = await axiosClient.get(`/user/groups/${id}/folders`, { params: { folderId } });
          setFolderContent(res.data);
          setCurrentFolderId(folderId);
      } catch (e) { messageApi.error("Lỗi tải tài liệu"); }
  };

  const handleCreateFolder = async (values: any) => {
      try {
          await axiosClient.post(`/user/groups/${id}/folders`, { name: values.name, parentId: currentFolderId });
          messageApi.success("Tạo thư mục thành công");
          setIsCreateFolderOpen(false);
          folderForm.resetFields();
          loadFolderContent(currentFolderId);
      } catch (e) { messageApi.error("Lỗi tạo thư mục"); }
  };

  // --- THÊM HÀM XÓA FOLDER ---
  const handleDeleteFolder = async (folderId: number) => {
      try {
          // Giả sử API xóa folder là DELETE /api/user/groups/{groupId}/folders/{folderId}
          // Bạn cần đảm bảo Backend có API này
          await axiosClient.delete(`/user/groups/${id}/folders/${folderId}`);
          messageApi.success("Đã xóa thư mục!");
          loadFolderContent(currentFolderId);
      } catch (e: any) {
          messageApi.error("Lỗi xóa thư mục (Có thể thư mục không rỗng hoặc không có quyền)");
      }
  };

  // Upload file dùng fetch để tránh lỗi Multipart của Axios
  const handleUploadFile = async (options: any) => {
      if (!currentFolderId) {
          messageApi.warning("Vui lòng vào một thư mục để upload (Không upload ở gốc)!");
          return;
      }
      const { file, onSuccess, onError } = options;
      const formData = new FormData();
      formData.append('file', file);
      const token = localStorage.getItem('token');

      try {
          const response = await fetch(`http://localhost:8080/api/user/groups/${id}/folders/${currentFolderId}/files`, {
              method: 'POST',
              headers: { 'Authorization': `Bearer ${token}` }, // KHÔNG set Content-Type
              body: formData
          });

          if (!response.ok) throw new Error('Upload thất bại');

          messageApi.success("Upload thành công");
          if (onSuccess) onSuccess("Ok");
          loadFolderContent(currentFolderId);
      } catch (e: any) { 
          messageApi.error("Lỗi upload file");
          if (onError) onError(e);
      }
  };

  // --- XÓA FILE ---
  const handleDeleteFile = async (fileId: number) => {
      try {
          // Gọi API xóa file
          await axiosClient.delete(`/user/groups/${id}/files/${fileId}`);
          messageApi.success("Đã xóa file!");
          loadFolderContent(currentFolderId); // Tải lại danh sách
      } catch (e: any) {
          messageApi.error("Lỗi xóa file: " + (e.response?.data || "Lỗi server"));
      }
  };

  // Xử lý xem file
  const handleViewFile = (file: any) => {
      const fullUrl = `${API_BASE_URL}${file.url}`;
      
      if (file.type && file.type.includes('image')) {
          setPreviewFile({ url: fullUrl, type: 'image', name: file.name });
      } else if (file.type && file.type.includes('pdf')) {
          setPreviewFile({ url: fullUrl, type: 'pdf', name: file.name });
      } else {
          window.open(fullUrl, '_blank');
      }
  };

  const getFileIcon = (type: string) => {
      if (type?.includes('image')) return <FileImageOutlined style={{fontSize: 24, color: '#ff85c0'}} />;
      if (type?.includes('pdf')) return <FilePdfOutlined style={{fontSize: 24, color: '#ff4d4f'}} />;
      return <FileTextOutlined style={{fontSize: 24, color: '#597ef7'}} />;
  };

  // --- LOGIC CHAT ---
  const handleSendChat = async () => {
    if (!inputMsg.trim()) return;
    const tempMsg = inputMsg;
    setInputMsg('');
    try {
      const res = await axiosClient.post('/chat/send', { groupId: Number(id), content: tempMsg });
      setMessages((prev) => [...prev, res.data]);
      setTimeout(scrollToBottom, 50);
    } catch (error) {
      messageApi.error("Lỗi gửi tin nhắn!");
      setInputMsg(tempMsg);
    }
  };

  // --- LOGIC TASK ---
  const openCreateTaskModal = () => {
      setEditingTask(null);
      taskForm.resetFields();
      setIsTaskModalOpen(true);
  };

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

  const handleSaveTask = async (values: any) => {
    try {
      const payload = {
        ...values,
        groupId: Number(id),
        deadline: values.deadline ? values.deadline.format('YYYY-MM-DDTHH:mm:ss') : null,
      };
      
      if (editingTask) {
          await axiosClient.put(`/user/groups/tasks/${editingTask.id}`, payload);
          messageApi.success("Cập nhật thành công!");
          if (viewTask?.id === editingTask.id) setViewTask({ ...viewTask, ...payload, status: values.status });
      } else {
          await axiosClient.post('/user/groups/tasks', payload);
          messageApi.success("Giao việc thành công!");
      }
      
      setIsTaskModalOpen(false);
      taskForm.resetFields();
      loadGroupTasks();
    } catch (error: any) {
      messageApi.error(error.response?.data || "Lỗi xử lý công việc!");
    }
  };

  const handleDeleteTask = async (taskId: number) => {
      try {
          await axiosClient.delete(`/user/groups/tasks/${taskId}`);
          messageApi.success("Đã xóa!");
          loadGroupTasks();
      } catch (error: any) {
          messageApi.error(error.response?.data || "Lỗi xóa");
      }
  };

  const handleUpdateStatus = async (taskId: number, newStatus: string) => {
      try {
          await axiosClient.patch(`/user/tasks/${taskId}/status`, { status: newStatus });
          messageApi.success("Cập nhật trạng thái xong!");
          loadGroupTasks();
          if (viewTask && viewTask.id === taskId) setViewTask({ ...viewTask, status: newStatus as any });
      } catch (e: any) { messageApi.error("Lỗi cập nhật"); }
  };

  const openDetailModal = async (taskId: number) => {
    try {
        const res = await axiosClient.get(`/user/tasks/detail/${taskId}`);
        setViewTask(res.data);
        setIsDetailOpen(true);
    } catch (e) { messageApi.error("Không thể xem chi tiết"); }
  };

  // --- MEMBER LOGIC ---
  const handleAddMember = async (values: any) => {
      try {
          await axiosClient.post(`/user/groups/${id}/members`, { emailOrUsername: values.username });
          messageApi.success("Mời thành công!");
          setIsAddMemberOpen(false);
          loadMembers();
      } catch (e: any) { messageApi.error(e.response?.data || "Lỗi mời thành viên"); }
  };

  // --- TABLE CONFIG ---
  const taskColumns = [
    { title: 'Công việc', dataIndex: 'title', render: (t: string) => <b>{t}</b> },
    { title: 'Người làm', dataIndex: 'userId', render: (uid: number) => {
        const user = members.find(m => m.userId === uid);
        return user ? <Tag color="blue">{user.username}</Tag> : 'N/A';
    }},
    { title: 'Hạn chót', dataIndex: 'deadline', width: 100, render: (d: string) => d ? dayjs(d).format('DD/MM') : '-' },
    { title: 'TT', dataIndex: 'status', width: 80, render: (s: string) => <Tag color={s === 'DONE' ? 'green' : 'orange'}>{s}</Tag> },
    {
        title: '', width: 110, key: 'action',
        render: (_: any, record: Task) => (
            <div style={{display: 'flex', gap: 4}}>
                <Button size="small" icon={<EyeOutlined />} onClick={() => openDetailModal(record.id)} />
                {canEditTask(record) && <Button size="small" icon={<EditOutlined style={{color:'#1890ff'}}/>} onClick={() => openEditTaskModal(record)} />}
                {canDeleteTask() && (
                    <Popconfirm title="Xóa?" onConfirm={() => handleDeleteTask(record.id)}>
                        <Button size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                )}
            </div>
        )
    }
  ];

  const breadcrumbItems = [
    { title: <a onClick={(e) => {e.preventDefault(); loadFolderContent(null);}}><HomeOutlined /> Gốc</a> },
    ...(currentFolderId ? [{ title: folderContent.currentFolderName || '...' }] : [])
  ];

  return (
    <div style={{ height: 'calc(100vh - 100px)', display: 'flex', flexDirection: 'column' }}>
      {contextHolder}

      <Card style={{ marginBottom: 16, flexShrink: 0 }} styles={{ body: { padding: '12px 24px' } }}>
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
            <div>
                <h2 style={{margin: 0, color: '#1890ff'}}>{groupInfo?.name || 'Loading...'}</h2>
                <span style={{color: '#888'}}>{groupInfo?.description}</span>
            </div>
            <Avatar.Group max={{ count: 5 }}>
                {members.map(m => (
                    <Tooltip title={`${m.username} (${m.role})`} key={m.userId}>
                        <Avatar style={{backgroundColor: m.role === 'LEADER' ? '#f56a00' : '#87d068'}}>{m.username[0].toUpperCase()}</Avatar>
                    </Tooltip>
                ))}
            </Avatar.Group>
          </div>
      </Card>

      <Row gutter={[16, 16]} style={{ flex: 1, minHeight: 0 }}>
        
        {/* CỘT TABS (CÔNG VIỆC, FILE, MEMBER) -> ĐƯA SANG TRÁI VÀ TO HƠN */}
        <Col xs={24} lg={17} style={{ height: '100%' }}>
            <Card style={{ height: '100%' }} styles={{ body: { padding: 0, height: '100%' } }}>
                <Tabs defaultActiveKey="1" type="card" style={{height: '100%'}} 
                    items={[
                        {
                            key: '1', label: 'Công việc',
                            children: (
                                <div style={{padding: 12, height: '100%', overflowY: 'auto'}}>
                                    {isLeader() && <Button type="primary" block icon={<PlusOutlined />} onClick={openCreateTaskModal} style={{marginBottom: 10}}>Giao việc mới</Button>}
                                    <Table dataSource={groupTasks} columns={taskColumns} rowKey="id" pagination={false} size="small" scroll={{ x: 500 }} />
                                </div>
                            )
                        },
                        {
                            key: '2', label: 'Tài liệu',
                            children: (
                                <div style={{padding: 12, height: '100%', display: 'flex', flexDirection: 'column'}}>
                                    <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 10}}>
                                        <Breadcrumb items={breadcrumbItems} />
                                        <div style={{display: 'flex', gap: 5}}>
                                            <Button size="small" icon={<PlusOutlined />} onClick={() => setIsCreateFolderOpen(true)}/>
                                            <Upload customRequest={handleUploadFile} showUploadList={false}><Button size="small" icon={<UploadOutlined />} disabled={!currentFolderId}/></Upload>
                                        </div>
                                    </div>
                                    {currentFolderId && <div onClick={() => loadFolderContent(folderContent.parentFolderId)} style={{cursor: 'pointer', padding: '5px', color: '#1890ff', fontSize: 12}}><ArrowUpOutlined /> Lên cấp trên</div>}
                                    <div style={{flex: 1, overflowY: 'auto'}}>
                                        {[...folderContent.subFolders, ...folderContent.files].length === 0 ? (
                                            <Empty description="Trống" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                                        ) : (
                                            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                                                {/* Render Folders */}
                                                {folderContent.subFolders.map((item: any) => (
                                                    <div 
                                                        key={'folder_' + item.id}
                                                        style={{
                                                            display: 'flex', alignItems: 'center', gap: 12, padding: '10px', 
                                                            borderRadius: 8, cursor: 'pointer',
                                                            backgroundColor: '#f9f9f9', border: '1px solid #eee'
                                                        }}
                                                        onClick={() => loadFolderContent(item.id)}
                                                    >
                                                        <div style={{ fontSize: 24 }}><FolderOutlined style={{color: '#faad14'}} /></div>
                                                        <div style={{ flex: 1, minWidth: 0 }}>
                                                            <div style={{ fontWeight: 500 }}>{item.name}</div>
                                                            <div style={{ fontSize: 11, color: '#888' }}>Bởi {item.createdBy}</div>
                                                        </div>
                                                        {/* Nút xóa Folder (Thêm vào đây) */}
                                                        {(isLeader() || item.createdBy === myUsername) && (
                                                            <Popconfirm title="Xóa thư mục này?" onConfirm={(e) => { e?.stopPropagation(); handleDeleteFolder(item.id); }}>
                                                                <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={(e) => e.stopPropagation()} />
                                                            </Popconfirm>
                                                        )}
                                                    </div>
                                                ))}

                                                {/* Render Files */}
                                                {folderContent.files.map((item: any) => (
                                                    <div 
                                                        key={'file_' + item.id}
                                                        style={{
                                                            display: 'flex', alignItems: 'center', gap: 12, padding: '10px', 
                                                            borderRadius: 8, cursor: 'default',
                                                            backgroundColor: '#fff', border: '1px solid #eee'
                                                        }}
                                                        onClick={() => handleViewFile(item)} // Click xem file
                                                    >
                                                        <div style={{ fontSize: 24 }}>{getFileIcon(item.type)}</div>
                                                        <div style={{ flex: 1, minWidth: 0 }}>
                                                            <div style={{ fontWeight: 500 }}>{item.name}</div>
                                                            <div style={{ fontSize: 11, color: '#888' }}>Bởi {item.uploadedBy}</div>
                                                        </div>
                                                        
                                                        <div style={{ display: 'flex', gap: 5 }}>
                                                            <Tooltip title="Tải xuống">
                                                                <Button type="text" size="small" icon={<DownloadOutlined />} onClick={(e) => { e.stopPropagation(); window.open(`${API_BASE_URL}${item.url}`, '_blank'); }} />
                                                            </Tooltip>
                                                            {/* Nút Xóa File - Chỉ hiện nếu là Leader hoặc người upload */}
                                                            {(isLeader() || item.uploadedBy === myUsername) && (
                                                                <Popconfirm title="Xóa file này?" onConfirm={(e) => { e?.stopPropagation(); handleDeleteFile(item.id); }}>
                                                                    <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={(e) => e.stopPropagation()} />
                                                                </Popconfirm>
                                                            )}
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )
                        },
                        {
                            key: '3', label: 'Thành viên',
                            children: (
                                <div style={{padding: 12, height: '100%', overflowY: 'auto'}}>
                                    {isLeader() && <Button type="dashed" block icon={<UserAddOutlined />} onClick={() => setIsAddMemberOpen(true)} style={{marginBottom: 10}}>Mời thành viên</Button>}
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

        {/* CỘT CHAT -> ĐƯA SANG PHẢI VÀ NHỎ HƠN */}
        <Col xs={24} lg={7} style={{ height: '100%' }}>
          <Card title="Thảo luận nhóm" style={{ height: '100%', display: 'flex', flexDirection: 'column' }} styles={{ body: { flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', padding: 16 } }}>
            <div style={{ flex: 1, overflowY: 'auto', paddingRight: 10, marginBottom: 10 }}>
              {messages.length === 0 && <Empty description="Chưa có tin nhắn" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                {messages.map((msg, index) => {
                  const isMe = msg.sender.username === myUsername;
                  return (
                    <div key={index} style={{ textAlign: isMe ? 'right' : 'left', marginBottom: 12 }}>
                      <div style={{ fontSize: 11, color: '#999', marginBottom: 2, padding: '0 4px' }}>{!isMe && <strong>{msg.sender.username} • </strong>}{dayjs(msg.sentAt).format('HH:mm')}</div>
                      <span style={{ display: 'inline-block', padding: '8px 14px', borderRadius: 16, background: isMe ? '#1890ff' : '#f0f2f5', color: isMe ? '#fff' : '#333', maxWidth: '100%', wordBreak: 'break-word', textAlign: 'left' }}>{msg.content}</span>
                    </div>
                  );
                })}
              </div>
              <div ref={messagesEndRef} />
            </div>
            <div style={{ display: 'flex', gap: 5, paddingTop: 10, borderTop: '1px solid #f0f0f0' }}>
              <Input value={inputMsg} onChange={e => setInputMsg(e.target.value)} onPressEnter={handleSendChat} placeholder="Nhập tin..." style={{borderRadius: 20}} />
              <Button type="primary" shape="circle" icon={<SendOutlined />} onClick={handleSendChat} />
            </div>
          </Card>
        </Col>
      </Row>

      {/* --- MODALS --- */}
      <Modal title="Mời thành viên" open={isAddMemberOpen} onCancel={() => setIsAddMemberOpen(false)} footer={null}>
          <Form onFinish={handleAddMember} layout="vertical">
              <Form.Item name="username" label="Email/Username" rules={[{required: true}]}><Input /></Form.Item>
              <Button type="primary" htmlType="submit" block>Mời ngay</Button>
          </Form>
      </Modal>

      <Modal title="Tạo thư mục" open={isCreateFolderOpen} onCancel={() => setIsCreateFolderOpen(false)} footer={null}>
           <Form form={folderForm} onFinish={handleCreateFolder} layout="vertical">
               <Form.Item name="name" label="Tên thư mục" rules={[{required: true}]}><Input /></Form.Item>
               <Button type="primary" htmlType="submit" block>Tạo</Button>
           </Form>
      </Modal>

      <Modal title={editingTask ? "Cập nhật công việc" : "Giao việc mới"} open={isTaskModalOpen} onCancel={() => setIsTaskModalOpen(false)} footer={null}>
          <Form form={taskForm} onFinish={handleSaveTask} layout="vertical">
              <Form.Item name="title" label="Tiêu đề" rules={[{required: true}]}><Input /></Form.Item>
              <Form.Item name="description" label="Mô tả"><Input.TextArea rows={2} /></Form.Item>
              <Form.Item name="assignToUserId" label="Giao cho" rules={[{required: true}]}>
                  <Select placeholder="Chọn thành viên" disabled={!isLeader()}>
                      {members.map(m => (<Select.Option key={m.userId} value={m.userId}>{m.username} ({m.email})</Select.Option>))}
                  </Select>
              </Form.Item>
              <div style={{ display: 'flex', gap: 10 }}>
                  <Form.Item name="deadline" label="Hạn chót" style={{flex:1}}><DatePicker showTime style={{width: '100%'}} /></Form.Item>
                  <Form.Item name="priority" label="Ưu tiên" style={{flex:1}} initialValue="MEDIUM">
                      <Select><Select.Option value="HIGH">Cao</Select.Option><Select.Option value="MEDIUM">TB</Select.Option><Select.Option value="LOW">Thấp</Select.Option></Select>
                  </Form.Item>
              </div>
              {editingTask && <Form.Item name="status" label="Trạng thái"><Select><Select.Option value="TODO">Chưa làm</Select.Option><Select.Option value="IN_PROGRESS">Đang làm</Select.Option><Select.Option value="DONE">Hoàn thành</Select.Option></Select></Form.Item>}
              <Button type="primary" htmlType="submit" block>{editingTask ? "Lưu thay đổi" : "Giao việc"}</Button>
          </Form>
      </Modal>

      {/* Modal Xem Chi Tiết & Cập Nhật Nhanh */}
      <Modal title="Chi tiết công việc" open={isDetailOpen} onCancel={() => setIsDetailOpen(false)} footer={[<Button key="close" onClick={() => setIsDetailOpen(false)}>Đóng</Button>]}>
        {viewTask ? (
          <div>
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="Tiêu đề"><b style={{fontSize: 16}}>{viewTask.title}</b></Descriptions.Item>
                <Descriptions.Item label="Mô tả">{viewTask.description || 'Không có mô tả'}</Descriptions.Item>
                <Descriptions.Item label="Trạng thái"><Tag color={viewTask.status === 'DONE' ? 'green' : 'orange'}>{viewTask.status}</Tag></Descriptions.Item>
                <Descriptions.Item label="Hạn chót">{viewTask.deadline ? dayjs(viewTask.deadline).format('HH:mm DD/MM') : '-'}</Descriptions.Item>
                <Descriptions.Item label="Người làm">{members.find(m => m.userId === viewTask.userId)?.username}</Descriptions.Item>
              </Descriptions>
              {canEditTask(viewTask) && (
                  <div style={{marginTop: 20, paddingTop: 15, borderTop: '1px dashed #ccc', display: 'flex', gap: 10, justifyContent: 'center'}}>
                      <Button onClick={() => handleUpdateStatus(viewTask.id, 'IN_PROGRESS')}>Đang làm</Button>
                      <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => handleUpdateStatus(viewTask.id, 'DONE')}>Hoàn thành</Button>
                  </div>
              )}
          </div>
        ) : <p>Đang tải...</p>}
      </Modal>

      {/* Modal Preview File */}
      <Modal 
        open={!!previewFile} 
        onCancel={() => setPreviewFile(null)} 
        footer={null}
        width={800}
        centered
        title={previewFile?.name}
        styles={{ body: { padding: 0, height: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f0f2f5' }}}
      >
          {previewFile?.type === 'image' ? (
              <img src={previewFile.url} alt="Preview" style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} />
          ) : (
              <iframe src={previewFile?.url} title="PDF Preview" width="100%" height="100%" style={{border: 'none'}} />
          )}
      </Modal>
    </div>
  );
};

export default GroupDetail;