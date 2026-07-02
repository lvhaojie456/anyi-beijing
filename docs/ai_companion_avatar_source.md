# AI Companion Avatar Source

The AI companion avatar editor is adapted from the MIT licensed Avataaars open-source projects:

- `fangpenlin/avataaars`: https://github.com/fangpenlin/avataaars
- `fangpenlin/avataaars-generator`: https://github.com/fangpenlin/avataaars-generator

## What Was Reused

- The Avataaars option model and naming style:
  `topType`, `accessoriesType`, `hatColor`, `hairColor`, `facialHairType`, `facialHairColor`,
  `clotheType`, `clotheColor`, `graphicType`, `eyeType`, `eyebrowType`, `mouthType`, and `skinColor`.
- The original Avataaars color palettes for skin, hair, hat, and clothing colors.
- The editor structure of selecting composable avatar parts.

## What Was Adapted

- The original project is React/SVG. This app uses native Android Compose, so the preview is redrawn with Compose Canvas.
- A small `faceShape` app extension was added because the AI companion flow needs a more obvious face-shaping control.
- The avatar JSON is stored in `avatarStyleJson` and sent to the backend with the companion profile.
- Backend prompts summarize these fields so the AI companion can keep the selected persona/appearance context.

## License

The copied/adapted option model is compatible with this app under the MIT License. A license notice is included at:

`app/src/main/assets/open_source_licenses/avataaars.txt`
