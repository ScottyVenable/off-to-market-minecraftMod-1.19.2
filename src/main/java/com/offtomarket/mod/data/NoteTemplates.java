package com.offtomarket.mod.data;

import java.util.List;
import java.util.Random;

/**
 * Repository of note templates for each event type.
 * Each template has a subject and body with placeholders:
 *   {town}     - town display name
 *   {item}     - item display name
 *   {count}    - item count
 *   {coins}    - coin amount (formatted)
 *   {reward}   - reward amount (formatted)
 *   {player}   - player name
 *   {reason}   - failure reason
 */
public class NoteTemplates {

    private static final Random RANDOM = new Random();

    public record Template(String subject, String body, String sender) {}

    // ==================== DIPLOMAT FAILURE ====================

    private static final List<Template> DIPLOMAT_FAILURE = List.of(
            new Template(
                    "Diplomatic Inquiry Unsuccessful",
                    "Dear Trader,\n\nI regret to inform you that my negotiations in {town} regarding {count}x {item} were unsuccessful. The town council deliberated at length but ultimately declined our request. They cited limited supplies and prior commitments to their own citizens.\n\nI shall return shortly. Perhaps we can try a different approach.\n\nYour faithful diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Request Denied by {town}",
                    "Trader,\n\nBad news from {town}. They won't part with any {item} at this time. The merchants there were quite firm — apparently there has been a shortage and they need every last one for themselves.\n\nI'm heading back now. We might have better luck with another town.\n\nRegards,\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Negotiations Failed",
                    "To my esteemed employer,\n\nThe journey to {town} was pleasant enough, but the negotiations were not. They have no interest in selling {count}x {item}. The town elder practically laughed me out of the hall. Something about 'outsiders always wanting what they can't have.'\n\nBetter luck next time, I suppose.\n\nHumbly,\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Trade Proposal Rejected",
                    "Greetings,\n\nI spoke with the {town} trade guild regarding your request for {item}. Unfortunately, they voted unanimously to reject our proposal. The guildmaster mentioned that recent demand has made it impossible to spare any for outside traders.\n\nI am on my way home.\n\nYour obedient servant",
                    "Your Diplomat"
            ),
            new Template(
                    "A Difficult Day in {town}",
                    "Dear Trader,\n\nWhat a day. I arrived in {town} full of optimism, only to find their markets bare of {item}. Even the black market dealers had none. The whole town seems to be hoarding everything they can get their hands on.\n\nI'll be home soon. Perhaps we should look elsewhere.\n\nWearily yours,\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Doors Closed in {town}",
                    "Trader,\n\nThe merchants of {town} have refused our request for {count}x {item}. It seems they've had a run of bad luck with outside traders recently, and trust is in short supply. They told me — politely but firmly — to look elsewhere.\n\nI'm returning empty-handed. Sorry about that.\n\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "No Luck This Time",
                    "Hello,\n\nI tried my very best in {town}, but {item} is simply not available. The townsfolk were kind enough, offering me tea and bread, but no amount of charm could convince them to part with their goods.\n\nOnward to better fortunes, I hope.\n\nCheers,\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Council Vote: Declined",
                    "Official Notice:\n\nThe town council of {town} has reviewed the trade request for {count}x {item} submitted on your behalf. After careful consideration, the council has voted to decline the request at this time.\n\nReason: Insufficient surplus to fulfill external orders.\n\nYou may submit a new request at a later date.\n\n— {town} Trade Office",
                    "{town} Trade Office"
            ),
            new Template(
                    "Rough Reception in {town}",
                    "Boss,\n\nWell, that could have gone better. The folks in {town} were none too pleased to see a diplomat asking for {item} again. Apparently we're not the only ones who've been asking. Competition is fierce out there.\n\nI'm heading back. Don't shoot the messenger.\n\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Political Complications",
                    "Dear Trader,\n\nI'm afraid politics have gotten in the way. {town} is currently in a trade dispute with a neighboring settlement, and all exports of {item} have been temporarily suspended. My hands were tied.\n\nI recommend we wait a few days and try again.\n\nWith respect,\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "The Price Was Too High",
                    "Trader,\n\nI found {item} in {town}, but the asking price was astronomical — far beyond what any reasonable trader would pay. I declined on your behalf rather than bankrupt us both. These merchants know they have a monopoly and aren't shy about exploiting it.\n\nBetter deals await elsewhere.\n\nYour Diplomat",
                    "Your Diplomat"
            ),
            new Template(
                    "Weather Delays and Woes",
                    "Dear Employer,\n\nA terrible storm delayed my arrival in {town}, and by the time I got there, the market had closed for the season. The {item} stockpiles have been moved to secure storage, and no one has authority to release them until the town elder returns from pilgrimage.\n\nA frustrating trip, to say the least.\n\nYour Diplomat",
                    "Your Diplomat"
            )
    );

    // ==================== QUEST COMPLETED ====================

    private static final List<Template> QUEST_COMPLETED = List.of(
            new Template(
                    "Quest Reward Delivered!",
                    "Congratulations, Trader!\n\nThe goods you delivered to {town} have been received with great appreciation. As promised, your reward of {reward} has been deposited. The townspeople speak highly of your reliability.\n\nKeep up the excellent work!\n\n— {town} Quest Board",
                    "{town} Quest Board"
            ),
            new Template(
                    "Many Thanks from {town}",
                    "Dear Friend,\n\nOn behalf of all of {town}, I want to express our deepest gratitude for fulfilling our request for {count}x {item}. Your delivery came at just the right time — our stores were nearly empty.\n\nYour reward of {reward} is well earned.\n\nWith warmth,\nThe Mayor of {town}",
                    "Mayor of {town}"
            ),
            new Template(
                    "A Job Well Done",
                    "Trader,\n\nExcellent work on the {item} delivery. {town}'s craftsmen can finally resume their work thanks to your efforts. The quest board has been updated, and your payment of {reward} has been processed.\n\nWe look forward to working with you again.\n\n— {town} Merchants Guild",
                    "{town} Merchants Guild"
            ),
            new Template(
                    "Heroes Welcome in {town}",
                    "Esteemed Trader,\n\nWord of your generous delivery has spread through {town} like wildfire! The children are singing songs about the mysterious trader who brought {count}x {item} when all hope seemed lost.\n\nYour reward of {reward} barely covers our gratitude. You are always welcome here.\n\nJoyfully,\n{town} Town Council",
                    "{town} Town Council"
            ),
            new Template(
                    "Receipt of Goods Confirmed",
                    "OFFICIAL RECEIPT\n\nItem: {item} x{count}\nDestination: {town}\nStatus: DELIVERED\nPayment: {reward}\n\nThank you for your prompt service. This receipt serves as confirmation that all goods were received in satisfactory condition.\n\n— {town} Quartermaster",
                    "{town} Quartermaster"
            ),
            new Template(
                    "You've Saved the Festival!",
                    "Dear Trader,\n\n{town}'s annual harvest festival was nearly cancelled due to a shortage of {item}. Thanks to your timely delivery, the celebrations will go on! The whole town is grateful.\n\nEnclosed is your payment of {reward}, plus our eternal thanks.\n\nFestively yours,\n{town} Festival Committee",
                    "{town} Festival Committee"
            ),
            new Template(
                    "Quest Complete — Bonus Earned",
                    "Trader,\n\nNot only did you complete the quest for {count}x {item}, but you did so ahead of schedule! The merchants of {town} are impressed with your efficiency. Your reward of {reward} has been issued.\n\nWe've marked you as a preferred supplier.\n\n— {town} Trade Authority",
                    "{town} Trade Authority"
            ),
            new Template(
                    "Grateful Hearts in {town}",
                    "To our beloved Trader,\n\nThe families of {town} wanted you to know that the {item} you delivered has made a real difference in their lives. The elderly can stay warm, the children are fed, and the workshops are busy once more.\n\n{reward} is your official reward, but know that you've earned something money can't buy.\n\nGratefully,\nThe People of {town}",
                    "People of {town}"
            ),
            new Template(
                    "Delivery Acknowledged",
                    "Trader,\n\nThis is to confirm that your delivery of {count}x {item} to {town} has been logged and verified. Payment of {reward} has been authorized and dispatched to your Trading Post.\n\nYour trader reputation continues to grow.\n\n— Regional Trade Commission",
                    "Regional Trade Commission"
            ),
            new Template(
                    "A Toast to the Trader!",
                    "Friend!\n\nThe tavern in {town} erupted in cheers when your cart arrived with {item}. The barkeep says you drink free next time you visit! Your reward of {reward} is being sent, but the real reward is the friends you've made along the way.\n\nRaise a glass!\n— {town} Tavern Keeper",
                    "{town} Tavern Keeper"
            ),
            new Template(
                    "Order Fulfilled Successfully",
                    "Dear Trader,\n\nYour delivery of {count}x {item} has been received and distributed among the residents of {town}. Everything was in perfect condition. Your reward of {reward} has been sent.\n\nShould you wish to take on more work, our quest board always has opportunities.\n\n— {town} Administration",
                    "{town} Administration"
            ),
            new Template(
                    "From the Desk of the Elder",
                    "My dear Trader,\n\nIn my many years leading {town}, I've seen traders come and go. Few have been as dependable as you. The {item} delivery was exactly what we needed, and your reward of {reward} is well deserved.\n\nMay your roads be safe and your profits plentiful.\n\nThe Elder of {town}",
                    "Elder of {town}"
            )
    );

    // ==================== QUEST EXPIRED ====================

    private static final List<Template> QUEST_EXPIRED = List.of(
            new Template(
                    "Quest Expired",
                    "Notice:\n\nThe quest for {count}x {item} requested by {town} has expired. The deadline has passed, and the town has made alternative arrangements to source the goods.\n\nNo penalty has been assessed, but please try to complete future quests in a timely manner.\n\n— {town} Quest Board",
                    "{town} Quest Board"
            ),
            new Template(
                    "Time's Up on Your Quest",
                    "Trader,\n\nWe waited as long as we could, but {town} needed {item} urgently and the deadline has passed. They've sourced it from another trader. The quest has been cancelled and removed from your active list.\n\nNo hard feelings — there's always next time.\n\n— Regional Trade Commission",
                    "Regional Trade Commission"
            ),
            new Template(
                    "Deadline Missed",
                    "Dear Trader,\n\nWe regret to inform you that the quest to deliver {count}x {item} to {town} has been closed due to the deadline passing. {town} has expressed disappointment but understands the challenges of the trade.\n\nPlease accept new quests only if you can fulfill them in time.\n\n— Quest Administration",
                    "Quest Administration"
            ),
            new Template(
                    "A Missed Opportunity",
                    "Trader,\n\nThe window of opportunity for delivering {item} to {town} has closed. The town's festival has come and gone, and they managed without us. A shame, really — the reward would have been {reward}.\n\nOnward to the next opportunity.\n\n— Your Trading Post",
                    "Your Trading Post"
            ),
            new Template(
                    "Quest Cancelled — No Delivery",
                    "NOTICE OF CANCELLATION\n\nQuest: Deliver {count}x {item}\nDestination: {town}\nStatus: EXPIRED\n\nThe above quest has been removed from active listings. {town} has fulfilled its needs through alternative channels.\n\n— {town} Quartermaster",
                    "{town} Quartermaster"
            ),
            new Template(
                    "Too Late for {town}",
                    "Trader,\n\nI don't mean to scold, but {town} really needed that {item}. They waited and waited, but eventually had to pay triple to get it from a traveling merchant. The quest has expired.\n\nPlease be more mindful of deadlines going forward.\n\nSincerely,\nYour Trading Post Clerk",
                    "Trading Post Clerk"
            ),
            new Template(
                    "Unfulfilled Promise",
                    "Dear Trader,\n\nThe quest board in {town} has been updated. Your accepted quest for {count}x {item} has been marked as expired. While we understand that circumstances sometimes prevent timely delivery, the townsfolk were counting on you.\n\nWe hope to see better results next time.\n\n— {town} Town Council",
                    "{town} Town Council"
            ),
            new Template(
                    "Contract Lapsed",
                    "Official Notice:\n\nThe supply contract for {count}x {item} between your trading operation and {town} has lapsed due to non-delivery within the agreed timeframe. No penalties have been applied to your account.\n\nFuture contracts remain available at the quest board.\n\n— Trade Regulatory Office",
                    "Trade Regulatory Office"
            ),
            new Template(
                    "Winter Won't Wait",
                    "Trader,\n\n{town} needed {item} before the cold set in, and unfortunately the deadline has passed. The townspeople have made do with what they had. Your quest has been marked as expired.\n\nSometimes the road is longer than we plan for.\n\n— {town} Supply Master",
                    "{town} Supply Master"
            ),
            new Template(
                    "The Caravan Moved On",
                    "Hello Trader,\n\nThe merchant caravan that was going to help distribute {item} in {town} has moved on to the next settlement. Without your delivery, the whole arrangement fell through.\n\nThe quest has expired. No rewards will be issued.\n\n— Caravan Master",
                    "Caravan Master"
            ),
            new Template(
                    "Opportunity Lost",
                    "Trader,\n\n{town}'s need for {count}x {item} went unfulfilled. Another supplier stepped in at the last moment, though at a much higher cost to the town. Your quest has expired.\n\nYour reputation in {town} may take a small hit.\n\n— {town} Merchants Guild",
                    "{town} Merchants Guild"
            )
    );

    // ==================== SHIPMENT RECEIVED ====================

    private static final List<Template> SHIPMENT_RECEIVED = List.of(
            new Template(
                    "Shipment Arrived in {town}!",
                    "Good news, Trader!\n\nYour shipment of goods has arrived safely in {town}. The market stalls are being set up as we speak, and early interest looks promising. We'll keep you posted on sales.\n\nFair winds and good trade!\n\n— {town} Harbormaster",
                    "{town} Harbormaster"
            ),
            new Template(
                    "Goods Received at Market",
                    "Trader,\n\nYour goods have been unpacked and inspected at the {town} market. Everything is in excellent condition. The merchants are eager to display your wares.\n\nWe expect brisk sales. Stay tuned.\n\n— {town} Market Inspector",
                    "{town} Market Inspector"
            ),
            new Template(
                    "A Warm Welcome for Your Wares",
                    "Dear Trader,\n\nThe people of {town} were delighted to see your shipment arrive! Fresh goods from afar always cause a stir in the marketplace. Several merchants have already made inquiries.\n\nThis looks like it'll be a profitable venture.\n\n— {town} Market Manager",
                    "{town} Market Manager"
            ),
            new Template(
                    "Delivery Confirmed",
                    "DELIVERY NOTICE\n\nYour shipment to {town} has been received.\nCondition: Good\nMarket Status: Listed for sale\n\nAll items have been catalogued and placed on the market floor. You will be notified when sales commence.\n\n— {town} Warehouse",
                    "{town} Warehouse"
            ),
            new Template(
                    "Great Timing!",
                    "Trader!\n\nYou couldn't have timed this better. Your shipment arrived in {town} just as the weekly market opened. Shoppers are already lining up! Your goods are the talk of the town.\n\nI'll have a sales report for you soon.\n\n— {town} Market Crier",
                    "{town} Market Crier"
            ),
            new Template(
                    "Your Cart Has Arrived",
                    "Hello Trader,\n\nJust a quick note to let you know that the trading cart carrying your goods has rolled into {town}. The driver reports a smooth journey with no incidents. Everything is accounted for.\n\nSales will begin shortly.\n\n— Caravan Dispatch",
                    "Caravan Dispatch"
            ),
            new Template(
                    "Market Buzz",
                    "Trader,\n\nWord on the street in {town} is that a new shipment just arrived with some impressive goods. That would be yours! The market vendors are already adjusting their prices to compete.\n\nYou've made quite the impression.\n\n— {town} Trade Correspondent",
                    "{town} Trade Correspondent"
            ),
            new Template(
                    "Safe and Sound",
                    "Dear Trader,\n\nI'm pleased to report that your shipment reached {town} without any trouble. The roads were clear, the weather was kind, and every last item arrived intact.\n\nMay your profits be as smooth as the journey.\n\n— Your Trading Cart Driver",
                    "Trading Cart Driver"
            ),
            new Template(
                    "Fresh Stock on the Shelves",
                    "Trader,\n\nThe shopkeepers of {town} have been restocking their shelves with your shipment all morning. Business is booming, and your goods are flying off the displays.\n\nExcellent quality, as always.\n\n— {town} Shopkeepers Association",
                    "{town} Shopkeepers Association"
            ),
            new Template(
                    "Another Successful Delivery",
                    "Hello!\n\nJust confirming — your latest shipment to {town} has been received and processed. The customs inspector gave everything the green light. No issues whatsoever.\n\nAnother successful delivery in the books!\n\n— {town} Customs Office",
                    "{town} Customs Office"
            ),
            new Template(
                    "The Merchants Are Pleased",
                    "Trader,\n\nThe merchant council of {town} has reviewed your shipment and they're quite impressed with the variety and quality. Several have expressed interest in establishing a regular trade arrangement.\n\nYour reputation here grows with every delivery.\n\n— {town} Merchant Council",
                    "{town} Merchant Council"
            ),
            new Template(
                    "Shipment Log Entry",
                    "SHIPMENT LOG — {town}\n\nStatus: Received\nInspection: Passed\nMarket Placement: Complete\n\nAll goods have been verified against the manifest. Everything checks out. Your cart driver has been compensated for the journey.\n\nGood trading!\n\n— {town} Logistics Office",
                    "{town} Logistics Office"
            )
    );

    // ==================== PURCHASE MADE ====================

    private static final List<Template> PURCHASE_MADE = List.of(
            new Template(
                    "Purchase Confirmation",
                    "Dear Trader,\n\nThis letter confirms your purchase of {count}x {item} for {coins}. The goods are being prepared for transport and will arrive at your Trading Post shortly.\n\nThank you for your patronage!\n\n— Market Board Sales Desk",
                    "Market Board Sales Desk"
            ),
            new Template(
                    "Your Order Is On Its Way",
                    "Trader,\n\nGreat news! The {item} you ordered has been packed and dispatched. Our fastest cart is bringing {count} units straight to your door. Total cost: {coins}.\n\nEnjoy your purchase!\n\n— {town} Express Delivery",
                    "{town} Express Delivery"
            ),
            new Template(
                    "Receipt of Purchase",
                    "PURCHASE RECEIPT\n\nItem: {item}\nQuantity: {count}\nTotal Paid: {coins}\nSeller: {town}\n\nThank you for your business. Please keep this receipt for your records.\n\n— {town} Market Authority",
                    "{town} Market Authority"
            ),
            new Template(
                    "A Fine Choice!",
                    "Trader,\n\nExcellent taste! The {item} you purchased from {town} is top quality. {count} units for {coins} is a fair deal, if I do say so myself. The seller asked me to pass along their thanks.\n\nHappy trading!\n\n— Your Purchasing Agent",
                    "Purchasing Agent"
            ),
            new Template(
                    "Order Processed",
                    "Hello Trader,\n\nYour order for {count}x {item} has been processed by the {town} market. Payment of {coins} has been received and logged. The goods should arrive soon via the standard trade routes.\n\nAs always, we appreciate your business.\n\n— {town} Finance Office",
                    "{town} Finance Office"
            ),
            new Template(
                    "Market Board Transaction",
                    "TRANSACTION NOTICE\n\nBuyer: Your Trading Post\nItem: {item} x{count}\nCost: {coins}\nStatus: Shipped\n\nThis transaction has been recorded in the Regional Trade Ledger. If you have any disputes, please contact the Market Board directly.\n\n— Regional Trade Commission",
                    "Regional Trade Commission"
            ),
            new Template(
                    "Thanks for Buying Local!",
                    "Dear Trader,\n\nThe merchants of {town} wanted to personally thank you for purchasing {count}x {item}. Supporting local trade strengthens the entire region! Your {coins} payment helps keep their families fed.\n\nWe hope to see you at the market again soon.\n\n— {town} Chamber of Commerce",
                    "{town} Chamber of Commerce"
            ),
            new Template(
                    "Goods En Route",
                    "Trader,\n\nJust a heads up — the {item} you bought is currently making its way to your Trading Post. The cart left {town} this morning with {count} units. Cost was {coins}, as agreed.\n\nShould arrive within the expected delivery window.\n\n— Caravan Dispatch",
                    "Caravan Dispatch"
            ),
            new Template(
                    "Bargain of the Day",
                    "Hello!\n\n{count}x {item} for just {coins}? You got yourself a deal! The seller in {town} was happy to move the stock, and you're happy to receive it. Everybody wins.\n\nThe goods are on their way.\n\n— {town} Market Crier",
                    "{town} Market Crier"
            ),
            new Template(
                    "Investment Opportunity",
                    "Dear Trader,\n\nYou've made a wise purchase. {item} from {town} is known for its quality, and {count} units at {coins} represents good value. Reselling these could yield a tidy profit.\n\nSmart trading!\n\n— Market Analysis Bureau",
                    "Market Analysis Bureau"
            ),
            new Template(
                    "Your Acquisition",
                    "Trader,\n\nConfirmation that your acquisition of {count}x {item} from the {town} market has been finalized. Total expenditure: {coins}. The goods are in transit and should reach you without delay.\n\nMay this purchase serve you well.\n\n— {town} Trade Registrar",
                    "{town} Trade Registrar"
            ),
            new Template(
                    "Seller's Gratitude",
                    "Dear Buyer,\n\nThe merchant from {town} who sold you {count}x {item} wanted me to pass along their thanks. They said {coins} was a fair price and they look forward to future transactions with you.\n\nGood relations make for good trade!\n\n— Market Board Liaison",
                    "Market Board Liaison"
            )
    );

    // ==================== Template Selection ====================

    /**
     * Get a random template for the given note type.
     */
    public static Template getRandomTemplate(MailNote.NoteType type) {
        List<Template> templates = switch (type) {
            case DIPLOMAT_FAILURE -> DIPLOMAT_FAILURE;
            case QUEST_COMPLETED -> QUEST_COMPLETED;
            case QUEST_EXPIRED -> QUEST_EXPIRED;
            case SHIPMENT_RECEIVED -> SHIPMENT_RECEIVED;
            case PURCHASE_MADE -> PURCHASE_MADE;
        };
        return templates.get(RANDOM.nextInt(templates.size()));
    }

    /**
     * Create a MailNote from a template, substituting placeholders.
     *
     * @param type      The note type
     * @param town      Town name (or empty string if N/A)
     * @param item      Item name (or empty string if N/A)
     * @param count     Item count (0 if N/A)
     * @param coins     Formatted coin string (or empty string if N/A)
     * @param reward    Formatted reward string (or empty string if N/A)
     * @param player    Player name (or empty string if N/A)
     * @param gameTime  Current game time for timestamp
     * @return A new MailNote with all placeholders replaced
     */
    public static MailNote createNote(MailNote.NoteType type, String town, String item,
                                       int count, String coins, String reward,
                                       String player, long gameTime) {
        Template template = getRandomTemplate(type);
        String subject = applyReplacements(template.subject(), town, item, count, coins, reward, player);
        String body = applyReplacements(template.body(), town, item, count, coins, reward, player);
        String sender = applyReplacements(template.sender(), town, item, count, coins, reward, player);
        return new MailNote(type, subject, body, sender, gameTime);
    }

    private static String applyReplacements(String text, String town, String item,
                                             int count, String coins, String reward, String player) {
        return text
                .replace("{town}", town)
                .replace("{item}", item)
                .replace("{count}", String.valueOf(count))
                .replace("{coins}", coins)
                .replace("{reward}", reward)
                .replace("{player}", player);
    }
}
