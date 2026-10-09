# Capturas de referencia — `onboarding-ingredients-safety-kitchen`

Dirección visual de los pasos 5–7 del onboarding (exportaciones aceptadas de Google Stitch). Ante
cualquier contradicción, manda `DESIGN.md`.

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

- `ingredientes/` — paso 5 "¿Qué te gusta cocinar?": dos grupos de chips en la misma pantalla
  ("Me gusta" en terracota / "Evito" en oliva), buscador "+ Añadir otro" y nota de exclusión mutua.
  Fuente: `docs/design/stitch-prompt.md` sección A (paso 5).
- `alergias/` — paso 6 "Alergias e intolerancias": banner ámbar, chips de alérgenos (ámbar, nunca
  rojo) y opción bien visible "Ninguna". Fuente: sección A (paso 6).
- `tu-cocina/` — paso 7 "Tu cocina": dificultad, tiempo disponible (Hasta 15/30/45/Más de 60 min) y
  stepper de comensales; CTA final "Empezar a cocinar". Fuente: sección A (paso 7).
