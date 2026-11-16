// Simulador de un canal externo (7.8, 10.3.1): se comporta como lo haría una agencia o una plataforma que vende los apartamentos
// del SGA. Usa SOLO la API del canal (/api/canal/**) con su credencial en X-Canal-Credencial; no necesita dependencias (Node 18+).
//
//   node simulador-canal/simulador.mjs                       escenario completo contra http://localhost:8080
//   node simulador-canal/simulador.mjs aceptada              solo una reserva aceptada
//   node simulador-canal/simulador.mjs conflicto             una reserva que choca: se rechaza y queda registrada
//   node simulador-canal/simulador.mjs repetido              el mismo mensaje dos veces: no duplica la reserva
//   node simulador-canal/simulador.mjs cancelacion           cancelar (idempotente) y cancelar lo que no existe
//
// Variables de entorno (todas opcionales):
//   SGA_URL                  URL base de la API (por defecto http://localhost:8080)
//   SGA_CANAL_CREDENCIAL     credencial del canal (por defecto la del canal de demostración DEMO; ver README)
//   SGA_ADMIN_CORREO / SGA_ADMIN_CLAVE   si se dan, al final se consulta la bitácora y los conflictos como administrador
//   SGA_FECHA_BASE           primer día de las estancias de prueba, AAAA-MM-DD (por defecto, 2031-02-03)
const URL_BASE = (process.env.SGA_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const CREDENCIAL = process.env.SGA_CANAL_CREDENCIAL ?? 'sgc_demo_simulador_canal_2026_no_usar_en_produccion';
const FECHA_BASE = process.env.SGA_FECHA_BASE ?? '2031-02-03';
const CORRIDA = Date.now().toString(36).toUpperCase(); // vuelve únicos los identificadores del canal en cada ejecución
const escenario = process.argv[2] ?? 'todos';

const sumarDias = (fecha, dias) => {
  const d = new Date(`${fecha}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + dias);
  return d.toISOString().slice(0, 10);
};
const persona = (n, nombre) => ({ documento: `SIM-${CORRIDA}-${n}`, nombre, fechaNacimiento: '1985-04-04' });
let fallos = 0;

async function llamar(metodo, ruta, cuerpo, cabeceras = { 'X-Canal-Credencial': CREDENCIAL }) {
  const respuesta = await fetch(URL_BASE + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...cabeceras },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    signal: AbortSignal.timeout(10_000),
  });
  const texto = await respuesta.text();
  let json = null;
  try {
    json = texto ? JSON.parse(texto) : null;
  } catch {
    /* no era JSON */
  }
  return { estado: respuesta.status, cuerpo: json, ubicacion: respuesta.headers.get('location') };
}

function verificar(descripcion, cumple, detalle = '') {
  if (!cumple) fallos++;
  console.log(`  ${cumple ? 'OK   ' : 'FALLÓ'} ${descripcion}${detalle ? ` — ${detalle}` : ''}`);
}
const titulo = (t) => console.log(`\n▶ ${t}`);

async function apartamentoLibre(entrada, salida, titular) {
  const r = await llamar('POST', '/api/canal/disponibilidad', { fechaEntrada: entrada, fechaSalida: salida, ocupantes: [titular] });
  if (r.estado === 401) throw new Error('Credencial rechazada (401): revise SGA_CANAL_CREDENCIAL o que el canal esté activo');
  verificar('consulta de disponibilidad', r.estado === 200 && r.cuerpo?.contenido?.length > 0, `HTTP ${r.estado}`);
  return r.cuerpo.contenido[0].identificacion;
}

const reserva = (id, apartamento, entrada, salida, titular) => ({
  identificadorExterno: id,
  identificacionApartamento: apartamento,
  fechaEntrada: entrada,
  fechaSalida: salida,
  correoTitular: 'simulador@correo.invalid',
  titular,
  ocupantes: [titular],
});

async function aceptada(ctx) {
  titulo('Reserva ACEPTADA: el canal reserva con su propio identificador');
  const r = await llamar('POST', '/api/canal/reservas', reserva(ctx.idA, ctx.apartamento, ctx.entrada, ctx.salida, ctx.paula));
  verificar('201 Created con Location', r.estado === 201 && r.ubicacion?.endsWith(`/api/canal/reservas/${ctx.idA}`), `HTTP ${r.estado}`);
  verificar('la reserva nace PENDIENTE con el identificador del canal', r.cuerpo?.estado === 'PENDIENTE' && r.cuerpo?.identificadorExterno === ctx.idA);
  ctx.codigoA = r.cuerpo?.codigoReserva;
  const c = await llamar('GET', `/api/canal/reservas/${ctx.idA}`);
  verificar('el canal consulta su reserva', c.estado === 200 && c.cuerpo?.codigoReserva === ctx.codigoA);
}

async function conflicto(ctx) {
  titulo('CONFLICTO (RN-18): otra reserva sobre las mismas noches se rechaza y no sobrescribe la vigente');
  const r = await llamar('POST', '/api/canal/reservas', reserva(ctx.idB, ctx.apartamento, sumarDias(ctx.entrada, 1), sumarDias(ctx.salida, 1), ctx.mario));
  verificar('409 con la regla violada', r.estado === 409 && /solapa/.test(r.cuerpo?.mensaje ?? ''), r.cuerpo?.mensaje);
  const vigente = await llamar('GET', `/api/canal/reservas/${ctx.idA}`);
  verificar('la reserva vigente sigue intacta', vigente.estado === 200 && vigente.cuerpo?.estado === 'PENDIENTE');
}

async function repetido(ctx) {
  titulo('MENSAJE REPETIDO (RN-19): el mismo identificador dos veces no duplica la reserva');
  const r = await llamar('POST', '/api/canal/reservas', reserva(ctx.idA, ctx.apartamento, ctx.entrada, ctx.salida, ctx.paula));
  verificar(
    '409 "El mensaje ya fue recibido" con el código de la reserva original',
    r.estado === 409 && /ya fue recibido/.test(r.cuerpo?.mensaje ?? '') && r.cuerpo.mensaje.includes(ctx.codigoA ?? ''),
    r.cuerpo?.mensaje,
  );
}

async function cancelacion(ctx) {
  titulo('CANCELACIÓN: idempotente, y solo de reservas propias');
  const r = await llamar('PUT', `/api/canal/reservas/${ctx.idA}/cancelar`, { motivo: 'El viajero cambió de planes (simulador)' });
  verificar('200 y la reserva queda CANCELADA', r.estado === 200 && r.cuerpo?.estado === 'CANCELADA', `HTTP ${r.estado}`);
  const otra = await llamar('PUT', `/api/canal/reservas/${ctx.idA}/cancelar`, {});
  verificar('cancelar otra vez responde 200 sin cambiar nada', otra.estado === 200 && otra.cuerpo?.estado === 'CANCELADA');
  const ajena = await llamar('PUT', `/api/canal/reservas/NO-EXISTE-${CORRIDA}/cancelar`, {});
  verificar('cancelar lo que no es del canal responde 404', ajena.estado === 404);
}

async function verificarComoAdministrador() {
  const correo = process.env.SGA_ADMIN_CORREO;
  const clave = process.env.SGA_ADMIN_CLAVE;
  if (!correo || !clave) {
    console.log('\n(Para ver la bitácora y los conflictos como administrador, defina SGA_ADMIN_CORREO y SGA_ADMIN_CLAVE.)');
    return;
  }
  titulo('Verificación como administrador: bitácora y conflictos');
  const login = await llamar('POST', '/api/auth/login', { correo, clave }, {});
  if (login.estado !== 200) {
    verificar('inicio de sesión del administrador', false, `HTTP ${login.estado}`);
    return;
  }
  const h = { Authorization: `Bearer ${login.cuerpo.token}` };
  const bit = await llamar('GET', '/api/canales/bitacora?canal=DEMO', undefined, h);
  const ops = (bit.cuerpo?.contenido ?? []).map((e) => `${e.operacion}:${e.resultado}:${e.codigoHttp}`);
  verificar('la bitácora registra los mensajes del canal', bit.estado === 200 && ops.length > 0, ops.slice(0, 4).join(' | '));
  const conf = await llamar('GET', '/api/reservas-externas/rechazos?revisado=false', undefined, h);
  verificar('el conflicto quedó registrado para revisión', conf.estado === 200 && (conf.cuerpo?.contenido ?? []).some((x) => x.identificadorExterno === `DEMO:${CORRIDA}-B`));
}

try {
  console.log(`Simulador de canal → ${URL_BASE}  (escenario: ${escenario}, corrida ${CORRIDA})`);
  const entrada = sumarDias(FECHA_BASE, 0);
  const salida = sumarDias(FECHA_BASE, 3);
  const ctx = { entrada, salida, paula: persona(1, 'Paula Nieto'), mario: persona(2, 'Mario Rojas'), idA: `${CORRIDA}-A`, idB: `${CORRIDA}-B` };
  ctx.apartamento = await apartamentoLibre(entrada, salida, ctx.paula);
  const necesitaReserva = ['conflicto', 'repetido', 'cancelacion'].includes(escenario);
  if (escenario === 'todos' || escenario === 'aceptada' || necesitaReserva) await aceptada(ctx);
  if (escenario === 'todos' || escenario === 'conflicto') await conflicto(ctx);
  if (escenario === 'todos' || escenario === 'repetido') await repetido(ctx);
  if (escenario === 'todos' || escenario === 'cancelacion') await cancelacion(ctx);
  if (escenario === 'todos') await verificarComoAdministrador();
  console.log(fallos === 0 ? '\nTodo coincide con lo esperado.' : `\n${fallos} verificación(es) fallaron.`);
  process.exitCode = fallos === 0 ? 0 : 1;
} catch (e) {
  console.error(`\nNo se pudo completar: ${e.message}`);
  process.exitCode = 2;
}
