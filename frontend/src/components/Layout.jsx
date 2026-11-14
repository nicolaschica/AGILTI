import { NavLink, Outlet } from 'react-router-dom';

export default function Layout() {
  const linkClass = ({ isActive }) => (isActive ? 'nav-link active' : 'nav-link');
  return (
    <>
      <header className="navbar">
        <div className="container navbar-inner">
          <NavLink to="/" className="brand">
            Agil<span>TI</span> <small>Helpdesk</small>
          </NavLink>
          <nav className="nav-links">
            <NavLink to="/" end className={linkClass}>Tickets</NavLink>
            <NavLink to="/tickets/nuevo" className={linkClass}>Crear ticket</NavLink>
            <NavLink to="/notificaciones" className={linkClass}>Notificaciones</NavLink>
          </nav>
        </div>
      </header>
      <main className="container main">
        <Outlet />
      </main>
    </>
  );
}
