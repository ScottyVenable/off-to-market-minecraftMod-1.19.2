package com.offtomarket.mod.data;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

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
                        ),
                        new Template(
                                        "Cultural Barriers",
                                        "Hello,\n\nI encountered some unexpected cultural barriers in {town}. They have a tradition of only trading {item} within their own community. Outsiders are seen as untrustworthy, and my attempts to negotiate were met with suspicion.\n\nI did my best, but it was a lost cause.\n\nBest,\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Arrested at the Gate",
                                        "Trader,\n\nThis is embarrassing to report, but I was briefly detained at the gates of {town} under suspicion of being a competing merchant's spy. By the time I was released, the market had closed and every seller of {item} had packed up for the night.\n\nI narrowly avoided a night in the stocks. Coming home now.\n\nMortified,\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Outbid by a Rival",
                                        "Boss,\n\nA rival trading house beat us to the punch. By the time I reached {town}'s market, every last unit of {item} had been snapped up by a well-dressed merchant with deeper pockets. I tried to negotiate a back-room deal but had no luck.\n\nWe need to move faster next time.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Supply Dried Up",
                                        "Trader,\n\nUnbelievable timing. {town} sold out of {item} just three days before I arrived. The shopkeeper showed me the empty crates and actually apologized. A drought upstream has disrupted the whole supply chain.\n\nI expect stocks will recover within the week, but for now there is nothing to be done.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Customs Refused Entry",
                                        "Dear Employer,\n\nThe customs office in {town} turned me away at the border checkpoint. New regulations apparently require a licensed trade medallion to negotiate bulk purchases of {item}. I was not aware of this requirement.\n\nI will look into obtaining the proper paperwork. Until then, the deal is off.\n\nApologetically,\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "The Merchant Was Away",
                                        "Hello Trader,\n\nOf all the rotten luck — the one merchant in {town} who stocks {item} in the quantities we need was away on a buying trip of their own. Their apprentice had no authority to sell and frankly didn't seem to know where anything was kept.\n\nI'll try again when they return.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Fire at the Warehouse",
                                        "Trader,\n\nI arrived in {town} to find the main warehouse smoldering. A fire tore through the storage district last night and most of the {item} stockpile was lost. The town is in mourning and in no mood to entertain trade talks.\n\nThe whole trip was a wash. I'm coming home.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Pride Got in the Way",
                                        "Dear Trader,\n\nThe guildmaster of {town} took personal offense at the price I offered for {item}. I attempted to explain that it was a fair market rate, but pride is a powerful thing. He called me a thief and threw me out.\n\nI'm not sure flattery or logic will work here. We may need a different envoy next time.\n\nSorry about the mess.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Town on Lockdown",
                                        "URGENT — Your Diplomat here.\n\nFound {town} under a full trade lockdown. Some kind of civic emergency involving a missing shipment of their own goods. All imports and exports suspended until the investigation concludes. There was nothing I could do about the {item} order.\n\nExpect normal operations to resume within a few days.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "They've Heard of Us",
                                        "Trader,\n\nApparently word has spread to {town} about our previous pricing disputes with nearby settlements. The moment I introduced myself, the trade captain recognized the name and refused to deal with us.\n\n{item} will have to wait until our reputation in the region improves. Something to consider.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Middleman Demanded",
                                        "Dear Employer,\n\nThe merchants of {town} insist on using their own certified middleman for all external transactions. The required intermediary fee would have eaten every bit of profit on {count}x {item}, so I walked away.\n\nUnless you are willing to pay the surcharge, I recommend finding a different supplier.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Guild Strike",
                                        "Trader,\n\n{town}'s Porters and Carriers Guild has gone on strike. Without their workers, nothing moves in or out of the market district. {item} is sitting in crates on the dock but may as well be on the moon for all the good it does us.\n\nI'll watch the situation and report back.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Religious Observance",
                                        "Hello,\n\nI forgot the calendar. {town} is in the middle of a week-long religious observance during which all commerce is strictly forbidden. The townsfolk were gracious hosts, but no business could be conducted whatsoever.\n\nI should have checked the dates before making the journey. Apologies for the wasted travel.\n\nYour Diplomat",
                                        "Your Diplomat"
                        ),
                        new Template(
                                        "Held to Ransom",
                                        "Confidential — Boss,\n\nThe merchant controlling {town}'s entire supply of {item} demanded I sign an exclusivity contract before he would sell a single unit. Agreeing would have locked us out of every other market for a full season. I refused.\n\nHis leverage was considerable, but the terms were extortion dressed as trade.\n\nYour Diplomat",
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
                        ),
                        new Template(
                                        "The Smiths Are Back to Work",
                                        "Trader!\n\nWith your delivery of {count}x {item}, the smiths of {town} fired up their forges this very morning for the first time in weeks. The ring of hammers on anvils is the finest music in the land. Your reward of {reward} is enclosed.\n\nCome visit sometime — we'll have something made for you.\n\n— {town} Blacksmiths' Brotherhood",
                                        "{town} Blacksmiths' Brotherhood"
                        ),
                        new Template(
                                        "Quest Fulfilled — Trade Rank Updated",
                                        "Official Notice:\n\nYour successful delivery of {count}x {item} to {town} has been recorded. Reward issued: {reward}. Additionally, your standing with {town} has been upgraded in the regional trade registry.\n\nHigher-tier quests are now available to you here.\n\n— Regional Trade Commission",
                                        "Regional Trade Commission"
                        ),
                        new Template(
                                        "A Letter from the Innkeeper",
                                        "Dear Trader,\n\nMy inn was overflowing with grateful chatter last night — everyone was talking about the trader who came through with {count}x {item}. The whole town feels safer knowing there are reliable folk out there.\n\nYour reward of {reward} is included with this letter.\n\nWarm regards,\n{town} Innkeeper",
                                        "{town} Innkeeper"
                        ),
                        new Template(
                                        "The Children Thank You",
                                        "Hello Trader!\n\nThe schoolmaster of {town} asked me to write this letter on behalf of the children. They were terribly worried about the shortage of {item} but thanks to you, everything is alright now. They drew pictures for you but I'm told they got a bit muddy on the way.\n\nYour reward is {reward}. The smiles are free.\n\n— {town} Schoolhouse",
                                        "{town} Schoolhouse"
                        ),
                        new Template(
                                        "Specialty Quest Cleared",
                                        "Trader,\n\nI won't lie — I wasn't sure you could pull it off. {count}x {item} is no small ask, and {town} had been waiting a long time. But here we are. Delivery confirmed, quest closed, reward of {reward} issued.\n\nI'll be recommending you to the other quest boards.\n\n— {town} Quest Registrar",
                                        "{town} Quest Registrar"
                        ),
                        new Template(
                                        "Harvest Relief",
                                        "Dear Friend of {town},\n\nThe harvest this year was poor, and we feared a hard winter ahead. Your delivery of {count}x {item} has given the town breathing room at last. The mood here has lifted considerably.\n\nPlease accept {reward} as a token of our appreciation and relief.\n\nGratefully,\n{town} Council of Elders",
                                        "{town} Council of Elders"
                        ),
                        new Template(
                                        "A Debt of Thanks",
                                        "Trader,\n\nThe townsfolk of {town} are not wealthy people, but they believe in honoring their debts. Your delivery of {count}x {item} fulfilled a pressing need, and your reward of {reward} has been gathered with everyone's contribution.\n\nYou have made real friends here.\n\n— {town} Community Fund",
                                        "{town} Community Fund"
                        ),
                        new Template(
                                        "Mission Accomplished",
                                        "Trader,\n\nMission accomplished. {count}x {item} delivered. {town} satisfied. Reward of {reward} issued. The quest board has been cleared.\n\nStraight to the point, as a good trade ought to be.\n\n— {town} Quartermaster",
                                        "{town} Quartermaster"
                        ),
                        new Template(
                                        "The Doctor Is Pleased",
                                        "Dear Trader,\n\nAs the physician of {town}, I want to personally thank you. The {item} you delivered is vital to several treatments I prescribe, and we were dangerously low. Your timely delivery may have saved lives.\n\nYour reward of {reward} has been authorized by the council.\n\nWith professional gratitude,\nDr. {town} Infirmary",
                                        "{town} Infirmary"
                        ),
                        new Template(
                                        "Night Shift Heroes",
                                        "Trader!\n\nThe night watch of {town} sends their regards! They received the {count}x {item} delivery just before sundown, which means they can keep the gates lit and the town safe all winter. Your reward of {reward} is well earned.\n\nSafe travels on those dark roads!\n\n— {town} Night Watch",
                                        "{town} Night Watch"
                        ),
                        new Template(
                                        "Construction Resumes",
                                        "Good tidings, Trader!\n\nWork on {town}'s new market hall had stalled for weeks because we were waiting on {item}. Thanks to your delivery, the builders are back on the scaffolding and the hall should be finished before the cold months arrive.\n\nYour reward of {reward} is included. Come see the finished building sometime!\n\n— {town} Building Committee",
                                        "{town} Building Committee"
                        ),
                        new Template(
                                        "Trade Guild Honours You",
                                        "Esteemed Trader,\n\nIt is my honour to inform you that the Trade Guild of {town} has voted to award you the title of Trusted Supplier. Your consistent deliveries, most recently {count}x {item}, have earned this distinction.\n\nYour reward of {reward} accompanies this letter.\n\nWith distinction,\nGrandmaster of the {town} Trade Guild",
                                        "{town} Trade Guild"
                        ),
                        new Template(
                                        "The Market Is Open Again",
                                        "Wonderful news, Trader!\n\nThanks to your delivery of {count}x {item}, the {town} market has fully reopened after weeks of partial closure. Buyers and sellers are flooding in from across the region. Energy here is incredible.\n\nYour reward of {reward} has been sent. You're the talk of the marketplace!\n\n— {town} Market Master",
                                        "{town} Market Master"
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
                        ),
                        new Template(
                                        "A Quiet Disappointment",
                                        "Trader,\n\nThe elder of {town} asked me to write this letter personally. She does not wish to assign blame — she understands that life is unpredictable. But the {item} never arrived, the deadline has passed, and the quest is now closed.\n\nShe hopes you will try again when circumstances allow.\n\n— {town} Elder's Secretary",
                                        "{town} Elder's Secretary"
                        ),
                        new Template(
                                        "The Festival Went Dark",
                                        "Dear Trader,\n\n{town}'s lantern festival was a dimmer affair this year without the {count}x {item} we needed. The children were disappointed, and the elders didn't say a word — which was somehow worse. The quest has expired.\n\nPerhaps next year will be different.\n\n— {town} Festival Warden",
                                        "{town} Festival Warden"
                        ),
                        new Template(
                                        "We Moved On Without You",
                                        "Trader,\n\nNo hard feelings, but {town} had to find another solution for {item}. It wasn't easy and it cost us more than it should have. The quest has been marked expired.\n\nWe would still welcome your trade in the future, but next time please only accept quests you know you can complete.\n\n— {town} Trade Board",
                                        "{town} Trade Board"
                        ),
                        new Template(
                                        "Ledger Closed",
                                        "ADMINISTRATIVE NOTICE:\n\nQuest ID: {item} x{count} for {town}\nStatus: EXPIRED — No delivery received before deadline.\n\nThis entry has been closed in the regional trade ledger. No reward will be disbursed. Your acceptance rate has been noted.\n\n— Trade Regulatory Office",
                                        "Trade Regulatory Office"
                        ),
                        new Template(
                                        "A Cold Reply from {town}",
                                        "Trader,\n\nThe merchants of {town} asked me to relay the following: the order for {count}x {item} was time-sensitive and was not fulfilled. They have found another source. The quest is expired.\n\nThey did not ask me to wish you well, but I will anyway.\n\n— {town} Trade Liaison",
                                        "{town} Trade Liaison"
                        ),
                        new Template(
                                        "Harvest Season Over",
                                        "Trader,\n\nHarvest season in {town} has ended, and with it the opportunity to deliver {item} in time. The farmers finished the work themselves, scraping by with what little they had. Your quest has expired.\n\nThe land waits for no one — not even the best of traders.\n\n— {town} Farming Cooperative",
                                        "{town} Farming Cooperative"
                        ),
                        new Template(
                                        "Ship Already Sailed",
                                        "Hello Trader,\n\nLiterally — the ship that was going to transport your {item} from {town} has already sailed without it. The goods were never delivered, the deadline passed, and the quest is now formally expired.\n\nBetter communication next time would help us all.\n\n— {town} Harbormaster",
                                        "{town} Harbormaster"
                        ),
                        new Template(
                                        "The Workshop Is Silent",
                                        "Trader,\n\nThe craftsmen of {town} waited as long as they could for {count}x {item}. Without it, the workshop fell silent and production halted entirely for the season. The quest has now expired.\n\nThe economic toll was considerable. We hope for better next time.\n\n— {town} Master Craftsman",
                                        "{town} Master Craftsman"
                        ),
                        new Template(
                                        "Another Trader Stepped In",
                                        "Trader,\n\nA passing merchant happened through {town} with {item} to spare and filled the order that you had accepted. Fortuitous for the town, less so for your quest record. The quest is now marked expired.\n\nQuick action matters in this trade. We hope you'll keep that in mind.\n\n— Regional Quest Bureau",
                                        "Regional Quest Bureau"
                        ),
                        new Template(
                                        "Notice of Non-Completion",
                                        "Dear Trader,\n\nThis is a formal notice that quest token for {count}x {item} bound for {town} has been voided due to the expiration of the fulfillment window. The quest reward of {reward} will not be disbursed.\n\nYour account remains in good standing. Please be aware that repeated non-completions may affect quest availability.\n\n— Trade Regulatory Office",
                                        "Trade Regulatory Office"
                        ),
                        new Template(
                                        "The Patients Waited",
                                        "Trader,\n\nI am the healer of {town}, and I am writing this myself because I believe in honest words. We needed {item} for our sick. We waited. It did not come. We found another way, but it was harder and slower.\n\nThe quest has expired. I bear no ill will. But I wanted you to know that these things have human faces.\n\n— Healer of {town}",
                                        "Healer of {town}"
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
                        ),
                        new Template(
                                        "Crowds at the Stalls",
                                        "Trader!\n\nI've never seen lines this long at the {town} market. Word spread fast that a fresh shipment was in, and the crowds came out in force. Your goods are moving fast. I expect we'll have numbers for you by end of day.\n\nSplendid timing on your part.\n\n— {town} Market Supervisor",
                                        "{town} Market Supervisor"
                        ),
                        new Template(
                                        "Dockside Receipt",
                                        "DOCKSIDE RECEIPT — {town} Harbor\n\nShipment Status: Unloaded and Inspected\nCargo Condition: Satisfactory\nMarket Transfer: In Progress\n\nYour goods have cleared the harbor and are being transported to the market district. Expect placement within the hour.\n\n— {town} Dock Authority",
                                        "{town} Dock Authority"
                        ),
                        new Template(
                                        "Early Morning Delivery",
                                        "Dear Trader,\n\nYour shipment rolled into {town} before sunrise, which meant the market stall holders had everything laid out and priced well before the morning rush. It was the talk of the early crowd.\n\nEarly deliveries get the best spots on the market floor. Well done.\n\n— {town} Morning Market Coordinator",
                                        "{town} Morning Market Coordinator"
                        ),
                        new Template(
                                        "Quality Compliments From the Buyers",
                                        "Trader,\n\nSeveral buyers at the {town} market stopped me specifically to compliment the quality of your goods. One merchant called them the finest she'd seen all season. High praise in a town with such discerning tastes.\n\nWell packed, well presented. A credit to your trading post.\n\n— {town} Market Liaison",
                                        "{town} Market Liaison"
                        ),
                        new Template(
                                        "Shipment Summary",
                                        "Hello Trader,\n\nYour shipment to {town} has been received, accounted for, and distributed to the appropriate market stalls. All documentation is in order.\n\nYour consistent record with us earns you priority placement at the next market fair.\n\n— {town} Trade Registry",
                                        "{town} Trade Registry"
                        ),
                        new Template(
                                        "The Guards Were Impressed",
                                        "Trader,\n\nEven the gate guards remarked on your shipment as it rolled into {town}! They're not usually ones to notice merchandise, but even they stopped to look. It must have been quite a sight on the road.\n\nAll goods safely received. Sales are underway.\n\n— {town} Gate Captain",
                                        "{town} Gate Captain"
                        ),
                        new Template(
                                        "Children Helped Unload",
                                        "Dear Trader,\n\nYour cart arrived in {town} during the afternoon school break, and half the children in town swarmed to help unload. The driver said they were more enthusiastic than the regular dock hands.\n\nEverything arrived in perfect order. Sales begin tomorrow morning.\n\n— {town} Market Steward",
                                        "{town} Market Steward"
                        ),
                        new Template(
                                        "Rival Traders Are Watching",
                                        "Confidential — Trader,\n\nYour shipment to {town} caused quite a stir among the local competition. Two rival stall owners were spotted watching closely as your goods were unloaded. The quality clearly has them worried.\n\nKeep it up. Your reputation here is growing.\n\n— {town} Market Intelligence",
                                        "{town} Market Intelligence"
                        ),
                        new Template(
                                        "Night Delivery Complete",
                                        "Trader,\n\nYour shipment arrived in {town} under cover of darkness — the night cart made excellent time on the empty roads. The warehouse crew processed everything by lantern light and had the stalls ready for morning.\n\nSmooth operation from start to finish.\n\n— {town} Night Logistics Crew",
                                        "{town} Night Logistics Crew"
                        ),
                        new Template(
                                        "All Present and Accounted For",
                                        "Trader,\n\nChecked every crate, counted every item — all {count} units present and accounted for. The {town} warehouse supervisor signed off without any issues. Your goods are now on the market floor.\n\nProfessional service as always.\n\n— {town} Inventory Clerk",
                                        "{town} Inventory Clerk"
                        ),
                        new Template(
                                        "Ahead of Schedule",
                                        "Dear Trader,\n\nYour shipment arrived in {town} two days earlier than expected! The market wasn't quite ready, but the stall holders hustled and had everything set up in record time. Nobody was complaining about extra inventory.\n\nEarly deliveries earn extra goodwill. Well done.\n\n— {town} Scheduling Office",
                                        "{town} Scheduling Office"
                        ),
                        new Template(
                                        "The Old Road Still Works",
                                        "Hello Trader,\n\nYour driver took the old mountain road into {town} rather than the main route, which apparently saved half a day of travel. Clever thinking. The goods arrived in excellent condition and ahead of schedule.\n\nWe've noted your route efficiency in our records.\n\n— {town} Logistics Commission",
                                        "{town} Logistics Commission"
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
                        ),
                        new Template(
                                        "You Cleared the Shelf",
                                        "Trader!\n\nYou bought the last of it! Your order of {count}x {item} wiped out the {town} market's entire remaining stock. The stall keeper was equal parts grateful and astonished. {coins} well spent.\n\nThe shelves will be restocked by next week if you need more.\n\n— {town} Stall Keeper",
                                        "{town} Stall Keeper"
                        ),
                        new Template(
                                        "Certified Authentic",
                                        "Dear Trader,\n\nThe {item} you purchased from {town} has been certified as authentic by the regional quality board. Your {coins} secured {count} units that have passed all inspections.\n\nTrading in certified goods protects your reputation. A wise choice.\n\n— Regional Quality Board",
                                        "Regional Quality Board"
                        ),
                        new Template(
                                        "Loaded on the Cart",
                                        "Trader,\n\nYour purchase of {count}x {item} has been loaded onto the outbound cart and is heading your way. Cost settled: {coins}. The driver has the manifest and knows the route well.\n\nExpect arrival within the usual timeframe.\n\n— {town} Cartage Office",
                                        "{town} Cartage Office"
                        ),
                        new Template(
                                        "Rare Find",
                                        "Dear Trader,\n\nYou have a good eye! The {item} you purchased from {town} is harder to find than most buyers realise. {count} units for {coins} is an excellent acquisition. Other traders had been eyeing it all week.\n\nYou got there first. Well played.\n\n— {town} Rare Goods Broker",
                                        "{town} Rare Goods Broker"
                        ),
                        new Template(
                                        "Prompt Payment Appreciated",
                                        "Trader,\n\nThe merchants of {town} wanted to specifically commend your prompt payment of {coins} for {count}x {item}. In a world full of slow payers and disputed invoices, your efficiency stands out.\n\nYou are always welcome at our market.\n\n— {town} Merchants' Collective",
                                        "{town} Merchants' Collective"
                        ),
                        new Template(
                                        "Packed With Care",
                                        "Dear Trader,\n\nThe {item} you ordered from {town} has been wrapped and packed with particular care for the journey. {count} units, total {coins}, now in transit. The packer is one of our best and takes pride in every shipment.\n\nExpect your goods in excellent condition.\n\n— {town} Packing House",
                                        "{town} Packing House"
                        ),
                        new Template(
                                        "Ledger Entry Confirmed",
                                        "TRADE LEDGER NOTICE:\n\nTransaction recorded — {town} to your Trading Post.\nGoods: {item} x{count}\nAmount paid: {coins}\nStatus: In transit\n\nThis entry is now part of the regional trade record. All taxes and fees have been settled.\n\n— Trade Regulatory Office",
                                        "Trade Regulatory Office"
                        ),
                        new Template(
                                        "The Seller Waves You Off",
                                        "Hello!\n\nThe old merchant who sold you {count}x {item} in {town} told me to write and say he hopes the goods treat you well. He's been in the trade for forty years and says buyers like you — who pay fair and ask smart — are the reason he keeps going.\n\n{coins} received. Goods on their way.\n\n— Market Messenger",
                                        "Market Messenger"
                        ),
                        new Template(
                                        "First Purchase Bonus",
                                        "Dear Trader,\n\nThis appears to be your first purchase from the {town} market! We hope it is the first of many. Your order of {count}x {item} for {coins} has been processed and the goods are en route.\n\nFirst-time buyers receive priority shipping from our warehouse. Welcome!\n\n— {town} Market Welcome Desk",
                                        "{town} Market Welcome Desk"
                        ),
                        new Template(
                                        "Midnight Bidding Won",
                                        "Trader,\n\nYou placed the winning bid on {count}x {item} at the {town} late auction. Final price: {coins}. Several other bidders were quite put out, but rules are rules — highest offer wins.\n\nThe goods will be dispatched first thing in the morning.\n\n— {town} Auction House",
                                        "{town} Auction House"
                        ),
                        new Template(
                                        "Restocked Just for You",
                                        "Dear Trader,\n\nYou'll be pleased to hear that the {town} market restocked {item} specifically in response to your inquiry last week. Your purchase of {count} units for {coins} was the first sale from the new stock.\n\nYour requests shape what we carry. Thank you for the business.\n\n— {town} Procurement Office",
                                        "{town} Procurement Office"
                        ),
                        new Template(
                                        "Courier Dispatched",
                                        "Trader,\n\nA dedicated courier has been dispatched from {town} carrying your {count}x {item}. Payment of {coins} has been confirmed on our end. The courier travels light and fast — expect a quicker arrival than the standard cart.\n\nSafe roads!\n\n— {town} Courier Service",
                                        "{town} Courier Service"
                        )
        );

        // Expanded pools (guarantee >= 20 templates per type)
        private static final List<Template> DIPLOMAT_FAILURE_POOL = ensureTwenty(DIPLOMAT_FAILURE, "Diplomat Report");
        private static final List<Template> QUEST_COMPLETED_POOL = ensureTwenty(QUEST_COMPLETED, "Quest Bulletin");
        private static final List<Template> QUEST_EXPIRED_POOL = ensureTwenty(QUEST_EXPIRED, "Quest Ledger");
        private static final List<Template> SHIPMENT_RECEIVED_POOL = ensureTwenty(SHIPMENT_RECEIVED, "Shipment Ledger");
        private static final List<Template> PURCHASE_MADE_POOL = ensureTwenty(PURCHASE_MADE, "Market Receipt");

        private static List<Template> ensureTwenty(List<Template> base, String flavor) {
                if (base.size() >= 20) return base;
                List<Template> expanded = new ArrayList<>(base);
                int variant = 1;
                while (expanded.size() < 20) {
                        Template source = base.get(variant % base.size());
                        expanded.add(new Template(
                                        source.subject() + " (" + flavor + " " + variant + ")",
                                        source.body() + "\n\nFiled under " + flavor + " archive #" + variant + ".",
                                        source.sender()
                        ));
                        variant++;
                }
                return expanded;
        }

        // ==================== Template Selection ====================

        /**
         * Get a random template for the given note type.
         */
        public static Template getRandomTemplate(MailNote.NoteType type) {
                List<Template> templates = switch (type) {
                        case DIPLOMAT_FAILURE -> DIPLOMAT_FAILURE_POOL;
                        case QUEST_COMPLETED -> QUEST_COMPLETED_POOL;
                        case QUEST_EXPIRED -> QUEST_EXPIRED_POOL;
                        case SHIPMENT_RECEIVED -> SHIPMENT_RECEIVED_POOL;
                        case PURCHASE_MADE -> PURCHASE_MADE_POOL;
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
