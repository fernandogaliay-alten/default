# Capturas de referencia — `activity-summary`

Dirección visual de la sección "Resumen" (resumen de actividad) de la pestaña Perfil, exportaciones
aceptadas de Google Stitch. Ante cualquier contradicción, manda `DESIGN.md`.

Estas capturas son **entrada obligatoria antes de implementar** (política de capturas de UI,
`docs/design/concepts/README.md`): mientras falten, la feature `activity-summary` queda `blocked`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/<nombre>.png
<pantalla>/code.html
<pantalla>/DESIGN.md
```

## Pantallas esperadas

- `perfil-resumen/` — sección "Resumen" de la pestaña Perfil: dos tarjetas de estadística compactas al
  lado, misma anchura ("24" con etiqueta "recetas generadas" y "12" con etiqueta "favoritas"), en
  cifras tabulares, con la variante de contadores pequeños ("0 recetas generadas" / "0 favoritas") y
  situada entre la cabecera de identidad y la sección "Sugerencias". Generada con el prompt-corte
  `docs/design/stitch/21-perfil-resumen.md` (fuente: `docs/design/stitch-prompt.md` sección G,
  "Section Resumen").

## Estado

Completa (2026-10-11):

- `perfil-resumen/screen.png` — export de Stitch (PNG 2816×3594): claro, oscuro y variante de usuario
  nuevo (0 recetas / 0 favoritas).
- `perfil-resumen/code.html` — export de Stitch.
- `perfil-resumen/DESIGN.md` — hoja "Design System" que Stitch emite **con cada pantalla**. Stitch no la
  re-emitió para un corte de sección, así que se versiona la hoja compartida del sistema, que es
  byte-idéntica (md5 `8c19d758379fa8c5b8aa449ad9200743`) a la de `results-screen/resultados`,
  `recipe-detail-screen/detalle-receta` y `favorites/favoritos`. No hay contenido nuevo: es la hoja del
  design system de Stitch.
