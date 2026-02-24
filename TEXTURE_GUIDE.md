# Off to Market (Trading Deluxe) — Texture Guide

All textures go under `src/main/resources/assets/offtomarket/textures/`.
Placeholder files already exist at each path — replace them with your pixel art.

---

## Block Textures (16×16 px)

All block textures are standard 16×16 pixel PNGs.

| # | File Path | Description | Notes |
|---|-----------|-------------|-------|
| 1 | `textures/block/trading_post_front.png` | Trading Post — front face | The face the player sees when placing the block. Should look like a shopfront counter or merchant stall window. |
| 2 | `textures/block/trading_post_side.png` | Trading Post — side faces | Left, right, and back sides of the block. Wooden plank/log style. |
| 3 | `textures/block/trading_post_top.png` | Trading Post — top face | Top-down view. Could have a sign or awning look. |
| 4 | `textures/block/trading_bin.png` | Trading Bin — all faces | Uses `cube_all` model so this one texture is applied to all 6 faces. Should look like a wooden crate or barrel. |
| 5 | `textures/block/market_board_front.png` | Market Board — front face | The readable face. Should look like a notice board, bulletin board, or signboard with writing/papers pinned to it. |
| 6 | `textures/block/market_board_side.png` | Market Board — side faces | Left, right, and back. Wooden frame or backing. |
| 7 | `textures/block/market_board_top.png` | Market Board — top face | Top edge of the board. Thin wooden frame. |

**Total: 7 block textures**

---

## Item Textures (16×16 px)

Standard item sprites, 16×16 pixel PNGs with transparency.

| # | File Path | Description | Notes |
|---|-----------|-------------|-------|
| 1 | `textures/item/gold_coin.png` | Gold Coin | Worth 100 CP. Should look prestigious — shiny gold with an emblem/stamp. Has enchantment glint in-game (foil effect). |
| 2 | `textures/item/silver_coin.png` | Silver Coin | Worth 10 CP. Silver-colored coin, slightly less ornate than gold. |
| 3 | `textures/item/copper_coin.png` | Copper Coin | Worth 1 CP. Copper-colored coin, simple design. |
| 4 | `textures/item/ledger.png` | Ledger | A leather-bound book/ledger. Used to remotely access a linked Trading Post. Could show a quill or bookmark. |
| 5 | `textures/item/shipment_note.png` | Shipment Note | A piece of parchment/scroll. Left behind in the Trading Bin after items are shipped. Could have writing/seal on it. |

**Total: 5 item textures**

---

## GUI Textures (256×256 px)

GUI background textures used in container screens. These are 256×256 pixel PNGs.
Currently the screens draw colored rectangles over these, so these serve as the base layer.
You can design full GUI backgrounds and remove the `fill()` calls in the screen code later for a polished look.

| # | File Path | Dimensions Used | Description | Notes |
|---|-----------|-----------------|-------------|-------|
| 1 | `textures/gui/trading_post.png` | 256×222 | Trading Post GUI background | Main trading hub screen. Layout areas: town selector (top), shipment list (middle), trader level/XP bar (bottom-left), send/collect buttons (right), player inventory (bottom). |
| 2 | `textures/gui/trading_bin.png` | 176×166 | Trading Bin GUI background | Storage screen. Layout areas: 3×3 item grid (center-left at 62,17), price input field (right at 120,17), set price button (right at 120,34), player inventory (bottom). Standard container layout similar to vanilla crafting table. |
| 3 | `textures/gui/market_board.png` | 256×222 | Market Board GUI background | Read-only listing display. Layout areas: title bar (top), scrollable listing table with columns for Item/Town/Qty/Price (middle, 4px to 238px wide), scroll buttons (far right), refresh button (top-right), player inventory (bottom). |

**Total: 3 GUI textures**

---

## Summary

| Category | Count | Size |
|----------|-------|------|
| Block textures | 7 | 16×16 px |
| Item textures | 5 | 16×16 px |
| GUI textures | 3 | 256×256 px |
| **Total** | **15** | |

---

## Color Palette Suggestions

To keep a cohesive medieval trading theme:

- **Wood tones**: `#8B5A2B` (saddle brown), `#654321` (dark wood), `#B58B4F` (light oak)
- **Gold coin**: `#FFD700` (gold), `#DAA520` (darker gold edge)
- **Silver coin**: `#C0C0C0` (silver), `#A8A8A8` (shadow)
- **Copper coin**: `#CD7F32` (copper), `#8B5E3C` (shadow)
- **Parchment**: `#E4D0A2` (light parchment), `#C4A86C` (aged)
- **Leather (ledger)**: `#8B4513` (saddle brown), `#5C3310` (dark)
- **GUI backgrounds**: `#3B2E1E` (dark wood border), `#5C4A32` (wood panel), `#2A1F14` (dark board)

---

## Tips

- **Block textures**: Use Minecraft's vanilla textures as reference for style. The crafting table, barrel, and lectern are good references for this mod's blocks.
- **Item textures**: Look at vanilla emerald, gold ingot, and written book for style reference.
- **GUI textures**: Reference `assets/minecraft/textures/gui/container/` from the vanilla jar for layout conventions. Slot backgrounds are typically `#8B8B8B` bordered by `#373737`.
- **Tools**: [Aseprite](https://www.aseprite.org/), [Piskel](https://www.piskelapp.com/) (free/browser), or [Pixilart](https://www.pixilart.com/) are great for 16×16 pixel art.
