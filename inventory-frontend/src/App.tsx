import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import MainLayout from './components/MainLayout';
import Groups from './pages/Groups';       // Trang danh sách nhóm
import GroupDetail from './pages/GroupDetail'; // Trang chi tiết nhóm (Chat + Task)

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* --- NHÓM PUBLIC (Không cần đăng nhập) --- */}
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />

        {/* --- NHÓM PRIVATE (Cần đăng nhập & Có Menu) --- */}
        {/* MainLayout sẽ bọc các trang con bên trong, hiển thị Sidebar và Header */}
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          
          {/* Route cho chức năng Nhóm */}
          <Route path="/groups" element={<Groups />} />
          <Route path="/groups/:id" element={<GroupDetail />} />
        </Route>

        {/* Xử lý trang 404 nếu nhập sai link (Tùy chọn) */}
        <Route path="*" element={
            <div style={{textAlign: 'center', marginTop: 50}}>
                <h1>404 - Không tìm thấy trang</h1>
                <a href="/">Quay về trang chủ</a>
            </div>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;