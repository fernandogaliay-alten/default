# Capturas de referencia — `ai-search`

Dirección visual de la Búsqueda con IA desde la Home (bloque 2), exportaciones aprobadas de Google
Stitch. Ante cualquier contradicción, manda `DESIGN.md`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/<nombre>.png
<pantalla>/code.html
<pantalla>/DESIGN.md
```

## Pantalla capturada

- `busqueda-ia/` — bloque 2 de la Home ("barra de búsqueda con IA") y sus estados:
  - barra píldora con icono de destello, placeholder "¿Qué te apetece hoy?" y helper
    "Pídeselo a la IA: «algo con pollo y arroz»"; en claro y en oscuro;
  - estado **sin conexión**: la barra se muestra deshabilitada con "Sin conexión · la búsqueda no está
    disponible" y el resto de la Home sigue normal;
  - estado de **carga** al enviar (misma animación que el Generador, "Buscando recetas para ti…");
  - **Resultados con la consulta como subtítulo** ("Resultados de tu búsqueda" + la consulta entre
    comillas);
  - estado **de fallo**: conserva la consulta y muestra "No hemos podido buscar. Revisa tu conexión."
    con acción "Reintentar";
  - estado **bloqueado por el Perfil**: una consulta que contradice el patrón de dieta devuelve el
    estado vacío que lo explica, nunca la receta prohibida.

  Fuente: `docs/design/stitch/11-home-busqueda-ia.md` (generada con el prompt-corte listo para pegar en
  `stitch-prompt.md`); referencia `docs/design/stitch-prompt.md` sección B (bloque 2) y
  `docs/domain-model.md` (política de la Búsqueda con IA).

Stitch no generó variantes por estado. Los estados **sin conexión**, **carga**, **resultados con
subtítulo**, **fallo con "Reintentar"** y **bloqueado por el Perfil** (tarjeta ámbar, nunca roja) se
implementan a partir de esta misma dirección y se describen en `docs/specs/ai-search.md`.

El estado bloqueado por el Perfil no es demostrable en un smoke manual con el `FakeRecipeGenerator`
real (un Perfil vegano solo descarta "Pollo al ajillo", así que `accepted` no queda vacío); lo cubre
`AiSearchOutcomeTest` en JVM y se revisa contra esta dirección.

`stitch-prompt.md` (raíz de la feature) es el prompt de generación y el contexto; `README.md` es este
índice.
