import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import TicketsListPage from './pages/TicketsListPage';
import CrearTicketPage from './pages/CrearTicketPage';
import TicketDetallePage from './pages/TicketDetallePage';
import NotificacionesPage from './pages/NotificacionesPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<TicketsListPage />} />
        <Route path="tickets/nuevo" element={<CrearTicketPage />} />
        <Route path="tickets/:id" element={<TicketDetallePage />} />
        <Route path="notificaciones" element={<NotificacionesPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
