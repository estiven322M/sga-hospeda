import { lazy } from 'react';
import { Route, Routes } from 'react-router';
import { Layout } from './componentes/Layout';
import { FiltrosProvider } from './contexto/FiltrosProvider';
import { FiltrosReservasProvider } from './contexto/FiltrosReservasProvider';
import { useAuth } from './hooks/useAuth';
import Inicio from './paginas/Inicio';
import {
  ROLES_ADMINISTRACION,
  ROLES_BUSQUEDA,
  ROLES_HUESPED,
  ROLES_RECEPCION,
  ROLES_SERVICIO,
  ROLES_TODOS,
} from './rutas/acceso';
import { RutaProtegida } from './rutas/RutaProtegida';
import { RutaPublica } from './rutas/RutaPublica';
import { PATRONES } from './rutas/rutas';

// Carga diferida: el código de cada página se descarga la primera vez que se visita.
// lazy() necesita una importación dinámica de un módulo con export default.
const Login = lazy(() => import('./paginas/Login'));
const Registro = lazy(() => import('./paginas/Registro'));
const NoAutorizado = lazy(() => import('./paginas/NoAutorizado'));
const NoEncontrada = lazy(() => import('./paginas/NoEncontrada'));
const RecuperarContrasena = lazy(() => import('./paginas/RecuperarContrasena'));
const Perfil = lazy(() => import('./paginas/Perfil'));

// Huésped
const Buscar = lazy(() => import('./paginas/huesped/Buscar'));
const DetalleApartamento = lazy(() => import('./paginas/huesped/DetalleApartamento'));
const NuevaReservaHuesped = lazy(() => import('./paginas/huesped/NuevaReservaHuesped'));
const MisReservas = lazy(() => import('./paginas/huesped/MisReservas'));
const DetalleReserva = lazy(() => import('./paginas/huesped/DetalleReserva'));

// Recepción
const Reservas = lazy(() => import('./paginas/recepcion/Reservas'));
const NuevaReservaRecepcion = lazy(() => import('./paginas/recepcion/NuevaReservaRecepcion'));
const GestionReserva = lazy(() => import('./paginas/recepcion/GestionReserva'));
const CalendarioOcupacion = lazy(() => import('./paginas/recepcion/CalendarioOcupacion'));

// Personal de servicio
const Preparacion = lazy(() => import('./paginas/servicio/Preparacion'));

// Administración
const Alojamiento = lazy(() => import('./paginas/administracion/Alojamiento'));
const Apartamentos = lazy(() => import('./paginas/administracion/Apartamentos'));
const ApartamentoFormulario = lazy(() => import('./paginas/administracion/ApartamentoFormulario'));
const ImagenesApartamento = lazy(() => import('./paginas/administracion/ImagenesApartamento'));
const Bloqueos = lazy(() => import('./paginas/administracion/Bloqueos'));
const TemporadasTarifas = lazy(() => import('./paginas/administracion/TemporadasTarifas'));
const PoliticaCancelacion = lazy(() => import('./paginas/administracion/PoliticaCancelacion'));
const Canales = lazy(() => import('./paginas/administracion/Canales'));
const Conflictos = lazy(() => import('./paginas/administracion/Conflictos'));
const BitacoraCanal = lazy(() => import('./paginas/administracion/BitacoraCanal'));
const Usuarios = lazy(() => import('./paginas/administracion/Usuarios'));
const Notificaciones = lazy(() => import('./paginas/administracion/Notificaciones'));

/**
 * Todas las rutas de la aplicación, declaradas desde el paso J1 para que nadie tenga que editar este archivo:
 * cada paso solo llena el contenido de sus páginas. Los patrones salen de rutas/rutas.ts y los roles de
 * rutas/acceso.ts (la matriz de docs/seguridad.md). Cada grupo de <RutaProtegida> exige haber ingresado y, si se
 * indican roles, tener alguno de ellos; si no, lleva al login o a /no-autorizado.
 */
export default function App() {
  const { sesion } = useAuth();

  return (
    // key por usuario: quien ingrese después en el mismo navegador no hereda los filtros de la búsqueda anterior
    <FiltrosProvider key={sesion?.correo ?? 'anonimo'}>
    <FiltrosReservasProvider key={sesion?.correo ?? 'anonimo'}>
    <Routes>
      <Route element={<Layout />}>
        {/* Inicio se carga de inmediato: es la primera página que ve casi todo el mundo */}
        <Route index element={<Inicio />} />
        <Route path="no-autorizado" element={<NoAutorizado />} />

        {/* Solo sin sesión: con sesión llevan a la página inicial del rol */}
        <Route element={<RutaPublica />}>
          <Route path="login" element={<Login />} />
          <Route path="registro" element={<Registro />} />
          <Route path={PATRONES.recuperarContrasena} element={<RecuperarContrasena />} />
        </Route>

        {/* Mi perfil y el cambio de contraseña: cualquier usuario con sesión */}
        <Route element={<RutaProtegida roles={ROLES_TODOS} />}>
          <Route path={PATRONES.perfil} element={<Perfil />} />
        </Route>

        {/* Buscar y cotizar: huésped, recepción y administración */}
        <Route element={<RutaProtegida roles={ROLES_BUSQUEDA} />}>
          <Route path={PATRONES.buscar} element={<Buscar />} />
          <Route path={PATRONES.detalleApartamento} element={<DetalleApartamento />} />
        </Route>

        {/* Huésped: reservar a su nombre y sus propias reservas */}
        <Route element={<RutaProtegida roles={ROLES_HUESPED} />}>
          <Route path={PATRONES.nuevaReservaHuesped} element={<NuevaReservaHuesped />} />
          <Route path={PATRONES.misReservas} element={<MisReservas />} />
          <Route path={PATRONES.detalleReserva} element={<DetalleReserva />} />
        </Route>

        {/* Recepción */}
        <Route element={<RutaProtegida roles={ROLES_RECEPCION} />}>
          <Route path={PATRONES.reservas} element={<Reservas />} />
          <Route path={PATRONES.nuevaReservaRecepcion} element={<NuevaReservaRecepcion />} />
          <Route path={PATRONES.gestionReserva} element={<GestionReserva />} />
          <Route path={PATRONES.calendarioOcupacion} element={<CalendarioOcupacion />} />
        </Route>

        {/* Personal de servicio */}
        <Route element={<RutaProtegida roles={ROLES_SERVICIO} />}>
          <Route path={PATRONES.preparacion} element={<Preparacion />} />
        </Route>

        {/* Administración */}
        <Route element={<RutaProtegida roles={ROLES_ADMINISTRACION} />}>
          <Route path={PATRONES.alojamiento} element={<Alojamiento />} />
          <Route path={PATRONES.apartamentos} element={<Apartamentos />} />
          <Route path={PATRONES.apartamentoNuevo} element={<ApartamentoFormulario />} />
          <Route path={PATRONES.apartamentoEditar} element={<ApartamentoFormulario />} />
          <Route path={PATRONES.imagenesApartamento} element={<ImagenesApartamento />} />
          <Route path={PATRONES.bloqueos} element={<Bloqueos />} />
          <Route path={PATRONES.temporadasTarifas} element={<TemporadasTarifas />} />
          <Route path={PATRONES.politicaCancelacion} element={<PoliticaCancelacion />} />
          <Route path={PATRONES.canales} element={<Canales />} />
          <Route path={PATRONES.conflictosCanal} element={<Conflictos />} />
          <Route path={PATRONES.bitacoraCanal} element={<BitacoraCanal />} />
          <Route path={PATRONES.usuarios} element={<Usuarios />} />
          <Route path={PATRONES.notificaciones} element={<Notificaciones />} />
        </Route>

        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
    </FiltrosReservasProvider>
    </FiltrosProvider>
  );
}
