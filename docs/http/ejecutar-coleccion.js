// Cliente de línea de comandos del formato .http de IntelliJ: ejecuta cada solicitud en orden y corre sus bloques "> {% ... %}"
// con la misma API (client.test, client.assert, client.global, response.body, response.headers.valueOf).
// Uso:  node docs/http/ejecutar-coleccion.js docs/http/sga.http http://localhost:8080 [--dump volcado.json]
// Debe ejecutarse sobre la aplicación recién arrancada (la colección no es idempotente). Ver docs/http/resultado-corrida.md.
const fs = require("fs");
const [, , archivo, host] = process.argv;
const texto = fs.readFileSync(archivo, "utf8").replace(/\r\n/g, "\n");

// Una solicitud empieza en una línea "### ..." (o "###" solo). Las variables "@x = y" se leen en su sitio.
const lineas = texto.split("\n");
const bloques = [];
let actual = null;
const variables = { host };
for (const l of lineas) {
  const v = l.match(/^@(\w+)\s*=\s*(.+)$/);
  if (v) { variables[v[1]] = v[2].trim(); continue; }
  if (/^###/.test(l)) { actual = { titulo: l.replace(/^###\s*/, ""), lineas: [] }; bloques.push(actual); continue; }
  if (actual) actual.lineas.push(l);
}

const globales = {};
let pruebas = 0, falladas = 0, solicitudes = 0, sinPruebas = [];
const resumen = [];
const volcado = [];
const iDump = process.argv.indexOf("--dump");
const archivoVolcado = iDump > 0 ? process.argv[iDump + 1] : null;

function sustituir(s) {
  return s.replace(/\{\{(\w+)\}\}/g, (_, n) => (n in variables ? variables[n] : (n in globales ? globales[n] : "{{" + n + "}}")));
}

(async () => {
  for (const b of bloques) {
    // quitar comentarios iniciales (# ...) y vacíos; el título puede estar en un comentario "# 3.15b ..."
    let i = 0;
    while (i < b.lineas.length && (b.lineas[i].trim() === "" || b.lineas[i].startsWith("#"))) i++;
    if (i >= b.lineas.length) continue;
    const m = b.lineas[i].match(/^(GET|POST|PUT|DELETE|PATCH|OPTIONS)\s+(\S+)/);
    if (!m) continue;
    const metodo = m[1];
    const url = sustituir(m[2]);
    const headers = {};
    let j = i + 1;
    for (; j < b.lineas.length && b.lineas[j].trim() !== "" && !b.lineas[j].startsWith(">"); j++) {
      const h = b.lineas[j].match(/^([\w-]+):\s*(.*)$/);
      if (h) headers[h[1]] = sustituir(h[2]);
    }
    // cuerpo y script
    const resto = b.lineas.slice(j).join("\n");
    const idxScript = resto.indexOf("> {%");
    const cuerpoCrudo = (idxScript >= 0 ? resto.slice(0, idxScript) : resto).split("\n").filter(x => !x.startsWith("#")).join("\n").trim();
    const script = idxScript >= 0 ? resto.slice(idxScript).match(/>\s*\{%([\s\S]*?)%\}/)?.[1] : null;
    const cuerpo = cuerpoCrudo ? sustituir(cuerpoCrudo) : undefined;

    solicitudes++;
    let res, texto;
    try {
      res = await fetch(url, { method: metodo, headers, body: cuerpo });
      texto = await res.text();
    } catch (e) {
      console.log("ERROR de red en [" + b.titulo + "]: " + e.message);
      falladas++;
      continue;
    }
    let json = null;
    try { json = texto ? JSON.parse(texto) : null; } catch (e) { json = texto; }
    const response = { status: res.status, body: json, headers: { valueOf: (n) => res.headers.get(n) } };

    volcado.push({ titulo: b.titulo, metodo, url, status: res.status, tieneScript: !!script, body: json });
    // código esperado según el título (" -> 201", "[409 ...]") solo informativo
    if (script) {
      const client = {
        global: { set: (k, v) => { globales[k] = v; }, get: (k) => globales[k] },
        test: (nombre, fn) => {
          pruebas++;
          try { fn(); resumen.push("  ok   " + nombre); }
          catch (e) { falladas++; resumen.push("  FALLA " + nombre + " -> " + e.message + " | cuerpo: " + texto.slice(0, 300)); }
        },
        assert: (cond, msg) => { if (!cond) throw new Error(msg || "aserción fallida"); },
      };
      try { new Function("client", "response", script)(client, response); }
      catch (e) { falladas++; resumen.push("  ERROR en script de [" + b.titulo.slice(0, 60) + "]: " + e.message); }
    } else {
      sinPruebas.push(res.status + " " + b.titulo.slice(0, 80));
    }
  }
  if (archivoVolcado) fs.writeFileSync(archivoVolcado, JSON.stringify(volcado, null, 1));
  console.log(resumen.join("\n"));
  console.log("\nsolicitudes=" + solicitudes + " client.test=" + pruebas + " fallos=" + falladas + " sin client.test=" + sinPruebas.length);
  if (process.argv[4] === "--sin-pruebas") console.log(sinPruebas.join("\n"));
})();
