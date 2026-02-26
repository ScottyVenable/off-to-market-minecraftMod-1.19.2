VERSION 0.5.1
---
- Potion of regeneration is showing up as 5c base price which is way too low. ✅ (v0.5.2)
- Breaking a Trading Ledger block causes all items in it to spill out but keep them in the containers they came from, practically doubling the amount of items. ✅ (v0.5.2)
- I cant click on Beaus market in the Towns tab to select it. It wont let me select to view it. It shows up, but cant select it. ✅ (v0.5.2)
- Create a custom texture for the "Finance Table" and fix/optimize the UI (see screenshot). Add many different tabs and completely revamp its usage to more than just storing coins. Improve the withdrawing to type in how many gold, silver, and copper pieces to withdraw from the balance. Show a tab for "Transactions" that includes withdraws, deposits, sales, etc.
- Says not enough coins for this purchase when I have enough coins in the finance table. I should be able to withdraw funds from my finance table when making puchases as long as the trade hub is connected to it. ✅ (v0.5.3)
- When requesting an enchanted book from the store, make it so I am able to type in which enchantment I want on the book (more rare/heavier enchantments are more expensive). Same with tools if clicking a check box window when confirming the order for a tool, I can choose which tools I want enchanted and with what.







VERSION 0.6
- Saw a town that was desperate for Elyrtra, but was selling 9 of them? They wouldn't do that unless they were desprate.
- Quitting and coming back duplicates items in the Trading Ledger. ✅ (v0.5.4)
- Fix trade imbalances.
- Fix the Trade button on the town page not sending the trade.
- Implement an overhaul of markets/towns to have their own inventory of items that refreshes every X hours. This will allow for more dynamic trading and a better sense of supply and demand. The prices of items will fluctuate based on how many times they have been bought/sold, and the inventory will be restocked with new items periodically. This will also allow for the implementation of a "black market" that appears randomly with rare and powerful items at high prices. A mining town that has good stock of pickaxes may be able to aquire more ore then if they are low on required items. A farming town that has good stock of seeds may be able to produce more crops then if they are low on required items. This will create a more immersive and dynamic trading experience for players, as they will need to pay attention to the market and adjust their trading strategies accordingly.
- Introduce ways for the player to lose reputation with towns, such as by failing to complete quests or engaging in dishonest trading practices/too high prices for junk (come on... 2g for dirt? Its offensive!). This will add an extra layer of challenge and consequence to the trading system, as players will need to maintain good relationships with the towns in order to access their markets and get benefits. At higher reputation, they may send gifts from time to time or waive certain fees. Losing reputation could result in higher prices, fewer available items, or even being banned from trading with that town altogether. This will encourage players to think carefully about their trading decisions and to prioritize building strong relationships with the towns they trade with.
- Implement more features to the mailbox.
- Make sure to add the custom Enchanted items and Enchanted Book feature.


Fixes:
- src/main/java/com/offtomarket/mod/network/WithdrawBinItemPacket.java -  lines +46 to +48: This packet handler mutates ledger contents and gives items to the sender without validating that the sender is near the block (or has the menu open), unlike other server-bound packets in this commit. A modified client can send arbitrary BlockPos values and withdraw from any loaded ledger in the same dimension, enabling remote theft on multiplayer servers.
- When withdrawing a virtual slot to inventory, the code only updates toGive if the source container slot is still non-empty; otherwise it still hands the player the old snapshot copy. If the source item was removed between sync ticks (or by another player), this path creates items from stale cache data and duplicates inventory. The give/drop path should only run when a real source extraction succeeded.
- This change replaces the trading_bin registry ID with trading_ledger but does not keep an alias or add a missing-mappings remap handler, so existing worlds/items saved with offtomarket:trading_bin will deserialize as missing content after upgrade. Adding an explicit remap is needed to preserve player builds and inventories across versions.