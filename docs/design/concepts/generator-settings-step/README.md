# Capturas de referencia — `generator-settings-step`

Dirección visual del paso 2 del Generador (exportaciones aceptadas de Google Stitch). Ante cualquier
contradicción, manda `DESIGN.md`.

> **Backfill (PR #37):** esta feature se implementó antes de la política de capturas de UI. Faltan sus
> capturas; déjalas en las subcarpetas de abajo y registra el estado en `DESIGN.md`.

Convención (ver `docs/design/concepts/README.md`): una subcarpeta por pantalla con los **tres ficheros
que exporta Stitch**:

```
<pantalla>/*.png
<pantalla>/*.html
<pantalla>/design.md
```

## Pantallas esperadas

- `generador-ajustes/` — Generador paso 2 "Ajusta tu receta": grupos en tarjeta (Tiempo disponible,
  Tipo de comida, Comensales, Dificultad, Alergias a evitar en ámbar + "Editar en mi perfil", notas) y
  primario anclado "Generar recetas". Fuente: `docs/design/stitch-prompt.md` sección C (paso 2).
