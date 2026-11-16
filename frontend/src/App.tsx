import { lazy } from 'react';
import { Route, Routes } from 'react-router';
import { Layout } from './componentes/Layout';
import Inicio from './paginas/Inicio';
import { PATRONES } from './rutas/rutas';

// Carga diferida: el código de cada página se descarga la primera vez que se visita.
// lazy() necesita una importación dinámica de un módulo con export default.
const Login = lazy(() => import('./paginas/Login'));
const Registro = lazy(() => import('./paginas/Registro'));
const NoAutorizado = lazy(() => import('./paginas/NoAutorizado'));
const NoEncontrada = lazy(() => import('./paginas/NoEncontrada'));

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

// Personal de servicio
const Preparacion = lazy(() => import('./paginas/servicio/Preparacion'));

// Administración
const Apartamentos = lazy(() => import('./paginas/administracion/Apartamentos'));
const ApartamentoFormulario = lazy(() => import('./paginas/administracion/ApartamentoFormulario'));
const Bloqueos = lazy(() => import('./paginas/administracion/Bloqueos'));
const TemporadasTarifas = lazy(() => import('./paginas/administracion/TemporadasTarifas'));
const PoliticaCancelacion = lazy(() => import('./paginas/administracion/PoliticaCancelacion'));
const RechazosExternos = lazy(() => import('./paginas/administracion/RechazosExternos'));

/**
 * Todas las rutas de la aplicación. Están declaradas desde el paso J1 para que nadie tenga que editar este
 * archivo después: cada paso solo llena el contenido de sus páginas. Los patrones salen de rutas/rutas.ts.
 * La protección por rol (RutaProtegida) se agrega en el paso J2 envolviendo estos mismos grupos.
 */
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* Inicio se carga de inmediato: es la primera página que ve casi todo el mundo */}
        <Route index element={<Inicio />} />
        <Route path="login" element={<Login />} />
        <Route path="registro" element={<Registro />} />
        <Route path="no-autorizado" element={<NoAutorizado />} />

        {/* Huésped */}
        <Route path={PATRONES.buscar} element={<Buscar />} />
        <Route path={PATRONES.detalleApartamento} element={<DetalleApartamento />} />
        <Route path={PATRONES.nuevaReservaHuesped} element={<NuevaReservaHuesped />} />
        <Route path={PATRONES.misReservas} element={<MisReservas />} />
        <Route path={PATRONES.detalleReserva} element={<DetalleReserva />} />

        {/* Recepción */}
        <Route path={PATRONES.reservas} element={<Reservas />} />
        <Route path={PATRONES.nuevaReservaRecepcion} element={<NuevaReservaRecepcion />} />
        <Route path={PATRONES.gestionReserva} element={<GestionReserva />} />

        {/* Personal de servicio */}
        <Route path={PATRONES.preparacion} element={<Preparacion />} />

        {/* Administración */}
        <Route path={PATRONES.apartamentos} element={<Apartamentos />} />
        <Route path={PATRONES.apartamentoNuevo} element={<ApartamentoFormulario />} />
        <Route path={PATRONES.apartamentoEditar} element={<ApartamentoFormulario />} />
        <Route path={PATRONES.bloqueos} element={<Bloqueos />} />
        <Route path={PATRONES.temporadasTarifas} element={<TemporadasTarifas />} />
        <Route path={PATRONES.politicaCancelacion} element={<PoliticaCancelacion />} />
        <Route path={PATRONES.rechazosExternos} element={<RechazosExternos />} />

        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  );
}
