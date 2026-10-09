# Capturas de referencia — `profile-screen`

Dirección visual de la pestaña Perfil (exportaciones aceptadas de Google Stitch). Ante cualquier
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

- `perfil/` — pestaña "Perfil": cabecera de identidad (avatar + nombre + "Tu perfil" + "Editar"),
  "Mis preferencias", "Alergias e intolerancias" (card ámbar), "Mi cocina" y "Ajustes". Fuente:
  `docs/design/stitch-prompt.md` sección G.
- `perfil-editar/` — modo edición de identidad: campo "¿Cómo te llamamos?", swatches de avatar y
  guardado (no se puede guardar vacío) y las hojas inferiores que reutilizan los editores del
  onboarding. Fuente: sección G (edición de identidad).
