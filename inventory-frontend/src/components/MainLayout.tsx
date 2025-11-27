import React, { useEffect, useState, useRef } from 'react';
import { Layout, Menu, Avatar, Dropdown, Badge, notification, Popover, List, Typography, Empty, Modal, Button, Descriptions, Tooltip, Input } from 'antd';
import {
  UserOutlined,
  TeamOutlined,
  LogoutOutlined,
  BellOutlined,
  DashboardOutlined,
  SettingOutlined,
  ExclamationCircleOutlined,
  MailOutlined,
  IdcardOutlined,
  MessageOutlined,
  CloseOutlined,
  SendOutlined
} from '@ant-design/icons';
import { useNavigate, Outlet, useLocation } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import type { Group, ChatMessage } from '../types';

const { Header, Content, Sider } = Layout;
const { Text, Title } = Typography;

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const username = localStorage.getItem('username');

  // --- STATE GIAO DIỆN ---
  const [collapsed, setCollapsed] = useState(false);
  const [api, contextHolder] = notification.useNotification();
  const [notificationsList, setNotificationsList] = useState<any[]>([]);
  
  // State Profile
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [userProfile, setUserProfile] = useState<any>(null);

  // --- STATE CHAT HEADS ---
  const [showChatHeads, setShowChatHeads] = useState(false);
  const [myGroups, setMyGroups] = useState<Group[]>([]);
  const [unreadMap, setUnreadMap] = useState<Record<number, number>>({});
  
  const [activeChatId, setActiveChatId] = useState<number | null>(null);
  const activeChatIdRef = useRef<number | null>(null); // FIX: Ref để socket đọc được state mới nhất
  
  const [miniMessages, setMiniMessages] = useState<ChatMessage[]>([]);
  const [miniInput, setMiniInput] = useState('');
  
  const miniMessagesEndRef = useRef<HTMLDivElement>(null);
  const stompClientRef = useRef<any>(null);

  // Đồng bộ State -> Ref
  useEffect(() => {
      activeChatIdRef.current = activeChatId;
  }, [activeChatId]);

  // --- 1. CHECK TOKEN & INIT ---
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) { navigate('/'); return; }

    const initSystem = async () => {
      try {
          // Load Groups
          const resGroups = await axiosClient.get<Group[]>('/user/groups');
          setMyGroups(resGroups.data);
          if (resGroups.data.length > 0) connectGlobalSocket(resGroups.data);

          // Load Alerts
          const resAlerts = await axiosClient.get<any[]>('/user/alerts');
          if (resAlerts.data && resAlerts.data.length > 0) {
            setNotificationsList(resAlerts.data);
            resAlerts.data.forEach((item: any) => {
                api.warning({ message: '⏳ HẠN CHÓT', description: item.title || item, duration: 5 } as any);
            });
          }
      } catch (e) { console.error(e); }
    };
    initSystem();

    return () => {
        if (stompClientRef.current) stompClientRef.current.disconnect();
    };
  }, [navigate]);

  // --- 2. SOCKET LOGIC (CHAT TỔNG) ---
  const connectGlobalSocket = (groups: Group[]) => {
    const socket = new SockJS('http://localhost:8080/ws');
    const client = Stomp.over(socket);
    client.debug = null; 
    const token = localStorage.getItem('token');

    client.connect({ 'Authorization': `Bearer ${token}` }, () => {
        console.log("✅ Global Chat Connected");
        groups.forEach(group => {
            client.subscribe(`/topic/group/${group.id}`, (payload) => {
                const newMessage = JSON.parse(payload.body);
                handleIncomingMessage(newMessage, group.id);
            });
        });
    }, () => {});
    stompClientRef.current = client;
  };

  const handleIncomingMessage = (msg: ChatMessage, groupId: number) => {
    // FIX: Dùng Ref để lấy ID đang mở hiện tại
    const currentOpenId = activeChatIdRef.current;

    // Nếu là tin nhắn của mình
    if (msg.sender.username === username) {
        if (currentOpenId === groupId) {
           setMiniMessages(prev => {
               if (prev.some(m => m.id === msg.id)) return prev;
               return [...prev, msg];
           });
           setTimeout(scrollMiniChatBottom, 100);
        }
        return;
    }

    // Tin nhắn người khác
    if (currentOpenId === groupId) {
        // Đang mở đúng nhóm đó -> Hiện tin nhắn
        setMiniMessages(prev => [...prev, msg]);
        setTimeout(scrollMiniChatBottom, 100);
    } else {
        // Đang đóng hoặc mở nhóm khác -> Hiện thông báo đỏ
        setUnreadMap(prev => ({ ...prev, [groupId]: (prev[groupId] || 0) + 1 }));
        setShowChatHeads(true); // Tự động bung list nhóm ra
        const audio = new Audio('https://www.soundjay.com/buttons/sounds/button-16.mp3');
        audio.play().catch(() => {});
    }
  };

  // Mở box chat mini
  const handleOpenMiniChat = async (groupId: number) => {
    if (activeChatId === groupId) {
        setActiveChatId(null); // Toggle tắt
        return;
    }
    setActiveChatId(groupId);
    setUnreadMap(prev => ({ ...prev, [groupId]: 0 })); // Xóa badge unread
    
    try {
        const res = await axiosClient.get(`/chat/history/${groupId}`);
        setMiniMessages(res.data);
        setTimeout(scrollMiniChatBottom, 100);
    } catch (e) { }
  };

  const scrollMiniChatBottom = () => {
    miniMessagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const handleSendMiniChat = async () => {
    if (!miniInput.trim() || !activeChatId) return;
    const tempMsg = miniInput;
    setMiniInput('');
    try {
        const res = await axiosClient.post('/chat/send', { groupId: activeChatId, content: tempMsg });
        setMiniMessages(prev => [...prev, res.data]);
        setTimeout(scrollMiniChatBottom, 50);
    } catch (e) { }
  };

  // --- 3. PROFILE & LOGOUT ---
  const handleOpenProfile = async () => {
    setIsProfileOpen(true);
    try {
      const res = await axiosClient.get('/user/profile');
      setUserProfile(res.data);
    } catch (error) {
      setUserProfile({ username: username, email: '...', role: 'VIEWER' });
    }
  };

  const handleLogout = async () => {
    try { await axiosClient.post('/auth/logout'); } catch(e) {}
    localStorage.clear();
    window.location.href = '/';
  };

  const userMenuItems = [
    { key: 'profile', label: 'Hồ sơ cá nhân', icon: <UserOutlined />, onClick: handleOpenProfile },
    { key: 'logout', label: 'Đăng xuất', icon: <LogoutOutlined />, danger: true, onClick: handleLogout },
  ];

  const notificationContent = (
    <div style={{ width: 300, maxHeight: 400, overflowY: 'auto' }}>
      {notificationsList.length === 0 ? (
        <Empty description="Không có thông báo mới" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {notificationsList.map((item: any, index: number) => (
            <div key={index} style={{ padding: '12px', background: '#fff1f0', borderRadius: '8px', borderLeft: '4px solid #ff4d4f', display: 'flex', gap: '12px' }}>
              <ExclamationCircleOutlined style={{ color: '#ff4d4f', fontSize: '20px', marginTop: '2px' }} />
              <div>
                <Text strong style={{ color: '#cf1322', display: 'block' }}>{typeof item === 'string' ? 'Thông báo' : `Quá hạn: ${item.title}`}</Text>
                <div style={{ fontSize: '12px', color: '#555' }}>{typeof item === 'string' ? item : `Hạn chót: ${item.deadline}`}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {contextHolder}

      {/* --- CHAT HEADS & MINI BOX --- */}
      <div style={{ position: 'fixed', right: 24, bottom: 24, zIndex: 2000, display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 12 }}>
          
          {/* Danh sách nhóm */}
          <div style={{ 
              display: 'flex', flexDirection: 'column', gap: 12, 
              marginBottom: 10, 
              opacity: showChatHeads ? 1 : 0, 
              transform: showChatHeads ? 'translateY(0)' : 'translateY(20px)',
              pointerEvents: showChatHeads ? 'auto' : 'none', 
              transition: 'all 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28)'
          }}>
              {myGroups.map(group => (
                  <Tooltip title={group.name} placement="left" key={group.id}>
                      <Badge count={unreadMap[group.id] || 0}>
                          <Avatar 
                            size={50} 
                            style={{ 
                                cursor: 'pointer', 
                                backgroundColor: '#fff', 
                                color: '#1890ff',
                                border: activeChatId === group.id ? '3px solid #1890ff' : '1px solid #d9d9d9',
                                boxShadow: '0 4px 10px rgba(0,0,0,0.1)'
                            }}
                            icon={<TeamOutlined />}
                            // Avatar tạo từ tên nhóm
                            src={`https://ui-avatars.com/api/?name=${group.name}&background=random&color=fff&size=128`}
                            onClick={() => handleOpenMiniChat(group.id)}
                          />
                      </Badge>
                  </Tooltip>
              ))}
          </div>

          {/* Nút Chat Toggle */}
          <Button 
            type="primary" 
            shape="circle" 
            size="large" 
            style={{ width: 60, height: 60, boxShadow: '0 6px 16px rgba(37, 99, 235, 0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            icon={showChatHeads ? <CloseOutlined style={{fontSize: 24}} /> : <MessageOutlined style={{fontSize: 24}} />}
            onClick={() => setShowChatHeads(!showChatHeads)}
          />
      </div>

      {/* Khung Chat Mini */}
      {activeChatId && (
          <div style={{
              position: 'fixed', right: 100, bottom: 24, 
              width: 340, height: 480, 
              background: '#fff', borderRadius: 16, 
              boxShadow: '0 8px 30px rgba(0,0,0,0.15)',
              display: 'flex', flexDirection: 'column',
              zIndex: 2001, overflow: 'hidden', border: '1px solid #f0f0f0',
              animation: 'slideIn 0.3s ease'
          }}>
              <div style={{ padding: '12px 16px', background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 600, fontSize: 15, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '250px' }}>
                      {myGroups.find(g => g.id === activeChatId)?.name}
                  </span>
                  <CloseOutlined onClick={() => setActiveChatId(null)} style={{ cursor: 'pointer' }} />
              </div>

              <div style={{ flex: 1, overflowY: 'auto', padding: 12, background: '#f9fafb', display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {miniMessages.map((msg, idx) => {
                      const isMe = msg.sender.username === username;
                      return (
                          <div key={idx} style={{ alignSelf: isMe ? 'flex-end' : 'flex-start', maxWidth: '85%' }}>
                              {!isMe && <div style={{ fontSize: 10, color: '#94a3b8', marginBottom: 2, marginLeft: 8 }}>{msg.sender.username}</div>}
                              <div style={{
                                  padding: '8px 14px',
                                  borderRadius: 18,
                                  background: isMe ? '#1890ff' : '#fff',
                                  color: isMe ? '#fff' : '#1e293b',
                                  boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                                  fontSize: 13,
                                  borderBottomRightRadius: isMe ? 4 : 18,
                                  borderBottomLeftRadius: isMe ? 18 : 4
                              }}>
                                  {msg.content}
                              </div>
                          </div>
                      )
                  })}
                  <div ref={miniMessagesEndRef} />
              </div>

              <div style={{ padding: 12, borderTop: '1px solid #eee', display: 'flex', gap: 8, background: '#fff' }}>
                  <Input 
                      value={miniInput} 
                      onChange={e => setMiniInput(e.target.value)} 
                      onPressEnter={handleSendMiniChat}
                      placeholder="Nhập tin nhắn..."
                      style={{ borderRadius: 20, background: '#f1f5f9', border: 'none' }}
                  />
                  <Button type="primary" shape="circle" icon={<SendOutlined />} onClick={handleSendMiniChat} />
              </div>
          </div>
      )}

      {/* --- LAYOUT CHÍNH --- */}
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} width={260} theme="light" breakpoint="lg" collapsedWidth="80" 
        style={{ boxShadow: '2px 0 8px rgba(0,0,0,0.05)', zIndex: 100, height: '100vh', position: 'fixed', left: 0, top: 0 }}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: '1px solid #f0f0f0' }}>
            {collapsed ? <h2 style={{ color: '#1890ff', margin: 0, fontWeight: 800, fontSize: '20px' }}>LSB</h2> : <h2 style={{ color: '#1890ff', margin: 0, fontWeight: 800, fontSize: '20px' }}>Todo-List</h2>}
        </div>
        
        <Menu mode="inline" selectedKeys={[location.pathname]} style={{ borderRight: 0, padding: '16px 8px' }} items={[
            { key: '/dashboard', icon: <DashboardOutlined />, label: 'Tổng quan & Task', onClick: () => navigate('/dashboard') },
            { key: '/groups', icon: <TeamOutlined />, label: 'Nhóm làm việc', onClick: () => navigate('/groups') },
            { type: 'divider' },
            { key: 'settings', icon: <SettingOutlined />, label: 'Cài đặt', disabled: true },
        ]} />
      </Sider>

      <Layout>
        <div style={{ marginLeft: collapsed ? 80 : 260, transition: 'margin-left 0.2s', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            
            <Header style={{ padding: '0 24px', background: 'rgba(255, 255, 255, 0.8)', backdropFilter: 'blur(8px)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 2px 4px rgba(0,0,0,0.02)', position: 'sticky', top: 0, zIndex: 99 }}>
                <span style={{ fontWeight: 600, fontSize: 18, color: '#333' }}>
                    {location.pathname.includes('/dashboard') ? 'Quản lý công việc' : 'Nhóm làm việc'}
                </span>
                
                <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
                    <Popover content={notificationContent} title="Thông báo quá hạn" trigger="click" placement="bottomRight">
                        <Badge count={notificationsList.length} overflowCount={99}>
                            <BellOutlined style={{ fontSize: 22, cursor: 'pointer', color: '#64748b' }} />
                        </Badge>
                    </Popover>
                    
                    <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" arrow>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
                            <span style={{ fontWeight: 500 }}>{username}</span>
                            <Avatar style={{ backgroundColor: '#1890ff' }} icon={<UserOutlined />} />
                        </div>
                    </Dropdown>
                </div>
            </Header>

            <Content style={{ margin: '24px', flex: 1 }}>
                <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
                    <div className="site-layout-content">
                         <Outlet />
                    </div>
                </div>
            </Content>

            <div style={{ textAlign: 'center', padding: '20px', color: '#aaa', fontSize: '12px' }}>
                ©2025 Inventory App System
            </div>
        </div>
      </Layout>

      {/* Modal Profile */}
      <Modal title="Thông tin cá nhân" open={isProfileOpen} onCancel={() => setIsProfileOpen(false)} footer={[<Button key="close" type="primary" onClick={() => setIsProfileOpen(false)}>Đóng</Button>]} centered width={400}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 24 }}>
          <Avatar size={80} style={{ backgroundColor: '#1890ff', marginBottom: 16 }} icon={<UserOutlined />} />
          <Title level={4} style={{ margin: 0 }}>{userProfile?.username || username}</Title>
          <Text type="secondary">Thành viên hệ thống</Text>
        </div>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label={<><UserOutlined /> Tên</>}>{userProfile?.username || username}</Descriptions.Item>
          <Descriptions.Item label={<><MailOutlined /> Email</>}>{userProfile?.email}</Descriptions.Item>
          <Descriptions.Item label={<><IdcardOutlined /> Vai trò</>}><Badge status="processing" text={userProfile?.role} color="blue"/></Descriptions.Item>
        </Descriptions>
      </Modal>
    </Layout>
  );
};

export default MainLayout;