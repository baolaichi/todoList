import React, {useEffect, useState}    from "react";
import { Card, Row, Col, Statistic, Table, Button, message, Popconfirm, Tag } from "antd";
import { UserOutlined, TeamOutlined, FileDoneOutlined, DeleteOutlined } from "@ant-design/icons";
import axiosClient from "../api/axiosClient";

const AdminDashboard: React.FC = () => {
    const [stats, setStats] = useState<any>({});
    const [users, setUsers] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const resStats = await axiosClient.get('/admin/stats');
            setStats(resStats.data);
            
            const resUsers = await axiosClient.get('/admin/users');
            setUsers(resUsers.data);
        } catch (error) {
            message.error("Bạn không có quyền truy cập Admin!");
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteUser = async (id: number) => {
        try {
            await axiosClient.delete(`/admin/users/${id}`);
            message.success("Đã xóa user!");
            loadData();
        } catch (e) {
            message.error("Lỗi khi xóa user");
        }
    };

    const columns = [
        { title: 'ID', dataIndex: 'id', key: 'id' },
        { title: 'Username', dataIndex: 'username', key: 'username', render: (text: string) => <b>{text}</b> },
        { title: 'Email', dataIndex: 'email', key: 'email' },
        { title: 'Vai trò', dataIndex: 'role', key: 'role', render: (role: string) => <Tag color={role === 'ADMIN' ? 'red' : 'blue'}>{role}</Tag> },
        {
            title: 'Thao tác',
            key: 'action',
            render: (_: any, record: any) => (
                record.role !== 'ADMIN' && ( // Không cho xóa chính mình hoặc admin khác
                    <Popconfirm title="Xóa user này?" onConfirm={() => handleDeleteUser(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Xóa</Button>
                    </Popconfirm>
                )
            )
        }
    ];

    return (
        <div style={{ padding: 24 }}>
            <h2>Quản trị hệ thống</h2>
            
            {/* Thống kê */}
            <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={8}>
                    <Card>
                        <Statistic title="Tổng User" value={stats.totalUsers} prefix={<UserOutlined />} valueStyle={{ color: '#3f8600' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic title="Tổng Nhóm" value={stats.totalGroups} prefix={<TeamOutlined />} valueStyle={{ color: '#cf1322' }} />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card>
                        <Statistic title="Tổng Task" value={stats.totalTasks} prefix={<FileDoneOutlined />} valueStyle={{ color: '#1890ff' }} />
                    </Card>
                </Col>
            </Row>

            {/* Bảng quản lý User */}
            <Card title="Danh sách người dùng">
                <Table 
                    dataSource={users} 
                    columns={columns} 
                    rowKey="id" 
                    loading={loading} 
                    pagination={{ pageSize: 5 }}
                />
            </Card>
        </div>
    );
};

export default AdminDashboard;