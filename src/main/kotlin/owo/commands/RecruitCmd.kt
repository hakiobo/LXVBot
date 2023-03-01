package owo.commands

import LXVBot
import LXVBot.Companion.LXV_DARK_TEAL
import LXVBot.Companion.LXV_HEDGE_SIGH_EMOJI
import LXVBot.Companion.LXV_MAGENTA
import LXVBot.Companion.LXV_SQUISH_EMOJI
import LXVBot.Companion.LXV_TEAL
import LXVBot.Companion.toOwODate
import commands.util.BotCommand
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import entities.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.litote.kmongo.combine
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import kotlin.math.max

object RecruitCmd : BotCommand {
    override val name: String
        get() = "recruit"
    override val description: String
        get() = "Gives Recruitment info"

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        val user = mCE.message.author!!
        val userId = user.id
        val lxvUserCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        val lxvUser = getUserFromDB(userId, user, lxvUserCol)

        if (LXVBot.RECUIT_BAN_ROLE_ID in mCE.member!!.roleIds) {
            recruitBanResponse(this, mCE)
        } else if (LXVBot.LXV_MEMBER_ROLE_ID in mCE.member!!.roleIds) {
            lxvMemberResponse(this, mCE)
        } else if (lxvUser.serverData.recruitData == null) {
            if (LXVBot.LXV_RECRUIT_ROLE_ID in mCE.member!!.roleIds) {
                untrackedRecruitResponse(this, mCE)
            } else if (mCE.message.channelId == RECRUIT_CHANNEL_ID) { // new recruit
                val timestamp = mCE.message.id.timestamp
                val statCol = db.getCollection<UserDailyStats>(UserDailyStats.DB_NAME)
                val (curOwOs, curBattles) = getStats(this, mCE, statCol)
                val endTime = timestamp.plus(TIME_LIMIT_MS, DateTimeUnit.MILLISECOND)

                val recruitData =
                    RecruitData(
                        timestamp.toEpochMilliseconds(),
                        endTime.toEpochMilliseconds(),
                        curOwOs,
                        curOwOs + OWOS_NEEDED,
                        curBattles,
                        curBattles + BATTLES_NEEDED,
                        true,
                    )
                val timeLeft = endTime - timestamp
                reply(mCE.message) {
                    author {
                        icon = mCE.message.author?.avatar?.url
                        name = "${lxvUser.username}'s Recruitment"
                    }
                    description =
                        "Hi, you have now started your LXV Recruitment ${LXVBot.NEW_LXV_GIF_EMOJI.mention}\n" +
                                "${LXVBot.ARROW_EMOJI.mention}Please come back here when you have reached the __**Needed Stats**__ in order to get the <@&${LXV_MEMBER_ROLE_ID}> Role. ${LXVBot.LXV_PEEK_EMOJI.mention}\n" +
                                "${LXVBot.ARROW_EMOJI.mention}Make sure to **__not__ be a member/recruit** in any other **OwO clan.**\n" +
                                "${LXVBot.ARROW_EMOJI.mention}You will have ${TIME_LIMIT_MS / (24L * 60 * 60 * 1000)} days to finish your recruitment process or your <@&${LXV_RECRUIT_ROLE_ID}> role will be removed.\n" +
                                "Have fun grinding! ${LXVBot.LXV_HEDGE_EMOJI.mention}"


                    field {
                        this.name = "Current Stats"
                        this.value = "__**OwOs**__: $curOwOs\n__**Battles**__: $curBattles"
                        inline = true
                    }
                    field {
                        this.name = "Needed Stats"
                        this.value =
                            "__**OwOs**__: ${curOwOs + OWOS_NEEDED}\n__**Battles**__: ${curBattles + BATTLES_NEEDED}"
                        inline = true
                    }



                    footer {
                        val d = timeLeft.inWholeDays
                        val h = timeLeft.inWholeHours % 24
                        val m = timeLeft.inWholeMinutes % 60
                        val s = timeLeft.inWholeSeconds % 60

                        text = "Recruitment Expires in ${d}D ${h}H ${m}M ${s}S"
                    }
                    color = LXVBot.LXV_PINK
                }
                val reminderCol = db.getCollection<StoredReminder>(StoredReminder.DB_NAME)
                client.launch {
                    lxvUserCol.updateOne(
                        LXVUser::_id eq lxvUser._id,
                        setValue(LXVUser::serverData / ServerData::recruitData, recruitData)
                    )
                    mCE.member!!.addRole(LXV_RECRUIT_ROLE_ID, "Started Recruitment")
                    val reminder = StoredReminder(
                        mCE.message.id,
                        endTime.toEpochMilliseconds(),
                        "recruitment",
                        "timeout",
                        mCE.message.channelId,
                        mCE.message.author!!.id,
                    )
                    reminderCol.insertOne(
                        reminder
                    )
                    delay(endTime - timestamp)
                    handleExpirationReminder(this@cmd, reminder, statCol, lxvUserCol)
                    reminderCol.deleteOne(StoredReminder::srcMsg eq reminder.srcMsg)
                }
            } else {
                redirectResponse(this, mCE)
            }
//        } else if (lxvUser.serverData.recruitData == null && mCE.message.channelId != RECRUIT_CHANNEL_ID) {
        } else { // check progress
            val timestamp = mCE.message.id.timestamp
            val (curOwOs, curBattles) = getStats(this, mCE)

            val recruitData = lxvUser.serverData.recruitData
            if (recruitData.active) {
                val owosNeeded = max(0L, recruitData.goalOwO - curOwOs)
                val battlesNeeded = max(0L, recruitData.goalBattle - curBattles)
                if (owosNeeded == 0L && battlesNeeded == 0L) { // recruitment completed!!!!! yayyyy
                    mCE.member!!.addRole(LXV_MEMBER_ROLE_ID, "Completed Recruitment")
                    mCE.member!!.removeRole(LXV_RECRUIT_ROLE_ID, "Completed Recruitment")
                    lxvUserCol.updateOne(
                        LXVUser::_id eq userId,
                        combine(
                            setValue(LXVUser::serverData / ServerData::recruitData, null),
                            setValue(LXVUser::serverData / ServerData::memberData, MemberData(curOwOs, curBattles))
                        )
                    )
                    sendMessage(MessageChannelBehavior(RECRUIT_CHANNEL_ID, client)) {
                        description =
                            "${user.mention} has become an official LXV Member!!! ${LXVBot.NEW_LXV_GIF_EMOJI.mention}"
                        field {
                            name = "Final Stats"
                            value = "OwOs: $curOwOs\n" +
                                    "Battles: $curBattles"
                        }
                        footer {
                            text = "these stat will be used for your level rewards\n"
                        }
                        color = LXVBot.LXV_MINT
                    }
                    reply(mCE.message) {
                        description =
                            "Congrats!!! Recruitment success, you are now an **LXV Member** ${LXVBot.LXV_HEDGE_WUV_EMOJI.mention}\n" +
                                    "You have unlocked **LXV Member’s exclusive perks!!** ${LXVBot.NEW_LXV_GIF_EMOJI.mention}"
                        color = LXVBot.LXV_PINK
                    }
                } else {
                    val endInstant = Instant.fromEpochMilliseconds(recruitData.endTime)
                    val timeLeft = endInstant - timestamp
                    reply(mCE.message) {
                        description =
                            "You still need **$owosNeeded OwO${if (owosNeeded == 1L) "" else "s"}** and **$battlesNeeded Battle${if (battlesNeeded == 1L) "" else "s"}** to become an LXV member.\n" +
                                    "You can do it!! ${LXVBot.LXV_HEDGE_HOLD_EMOJI.mention}\n"
                        footer {
                            val d = timeLeft.inWholeDays
                            val h = timeLeft.inWholeHours % 24
                            val m = timeLeft.inWholeMinutes % 60
                            val s = timeLeft.inWholeSeconds % 60

                            text = "Recruitment Expires in ${d}D ${h}H ${m}M ${s}S"
                        }
                        color = LXVBot.LXV_BRIGHT_TEAL
                    }
                }
            } else { // expired, rip
                lxvUserCol.updateOne(
                    LXVUser::_id eq userId,
                    setValue(LXVUser::serverData / ServerData::recruitData, null)
                )
                reply(mCE.message) {
                    description =
                        "Your recruitment has expired! ${LXVBot.LXV_CRY_EMOJI.mention} You can re-apply for it in <#${RECRUIT_CHANNEL_ID}>"
                    color = LXVBot.LXV_PINK
                }
            }
        }
    }

    private suspend fun getStats(
        bot: LXVBot,
        mCE: MessageCreateEvent,
        statCol: CoroutineCollection<UserDailyStats> = bot.db.getCollection(UserDailyStats.DB_NAME)
    ): Pair<Long, Long> {
        val timestamp = mCE.message.id.timestamp
        val todayDate = timestamp.toOwODate()
        val userId = mCE.message.author!!.id
        val guildId = mCE.guildId!!
        val curOwOsReq = bot.client.async {
            UserDailyStats.getStatInRange(
                statCol,
                userId,
                guildId,
                UserDailyStats.epoch,
                todayDate,
                UserDailyStats::owoCount,
            )
        }
        val curBattlesReq = bot.client.async {
            UserDailyStats.getStatInRange(
                statCol,
                userId,
                guildId,
                UserDailyStats.epoch,
                todayDate,
                UserDailyStats::battleCount,
            )
        }
        return curOwOsReq.await() to curBattlesReq.await()
    }

    private suspend fun lxvMemberResponse(bot: LXVBot, mCE: MessageCreateEvent) {
        bot.reply(mCE.message) {
            description =
                "Hmm, it seems like you already have the <@&${LXV_MEMBER_ROLE_ID}> role! ${LXV_SQUISH_EMOJI.mention}"
            color = LXV_TEAL
        }
    }

    private suspend fun recruitBanResponse(bot: LXVBot, mCE: MessageCreateEvent) {
        bot.reply(mCE.message) {
            description =
                "Oh no! It seems like you have been banned from becoming an LXV member! Try again when your ban is lifted. ${LXV_HEDGE_SIGH_EMOJI.mention}\n"
            color = LXV_MAGENTA
        }
    }

    private suspend fun untrackedRecruitResponse(bot: LXVBot, mCE: MessageCreateEvent) {
        bot.reply(mCE.message) {
            description =
                "Seems like you're recruiting without me :c"
            color = LXV_DARK_TEAL
        }
    }

    private suspend fun redirectResponse(bot: LXVBot, mCE: MessageCreateEvent) {
        bot.reply(
            mCE.message
        ) {
            color = LXVBot.LXV_TEAL
            description = "Hi! If you would like to recruit, please check out the information with `*member` first.\n" +
                    "After that, you may go to <#${RECRUIT_CHANNEL_ID}> and do the command `${LXVBot.BOT_PREFIX} $name` to start the recruitment process! ${LXVBot.LXV_HEART_EMOJI.mention}"
        }
    }


    suspend fun handleExpirationReminder(
        bot: LXVBot,
        reminder: StoredReminder,
        statCol: CoroutineCollection<UserDailyStats> = bot.db.getCollection(UserDailyStats.DB_NAME),
        lxvUserCol: CoroutineCollection<LXVUser> = bot.db.getCollection(LXVUser.DB_NAME),
    ) {
        val userId = reminder.otherData
        val lxvUser = bot.getUserFromDB(userId, null, lxvUserCol)
        val recruitData = lxvUser.serverData.recruitData
        if (recruitData?.startTime == reminder.srcMsg.timestamp.toEpochMilliseconds()) {
            val date = reminder.srcMsg.timestamp.plus(reminder.reminderTime, DateTimeUnit.MILLISECOND).toOwODate()
            val user = UserBehavior(userId, bot.client)
            val curOwOsReq = bot.client.async {
                UserDailyStats.getStatInRange(
                    statCol,
                    userId,
                    LXVBot.LXV_GUILD_ID,
                    UserDailyStats.epoch,
                    date,
                    UserDailyStats::owoCount,
                )
            }
            val curBattlesReq = bot.client.async {
                UserDailyStats.getStatInRange(
                    statCol,
                    userId,
                    LXVBot.LXV_GUILD_ID,
                    UserDailyStats.epoch,
                    date,
                    UserDailyStats::battleCount,
                )
            }
            val curBattles = curBattlesReq.await()
            val curOwOs = curOwOsReq.await()
            if (curBattles >= recruitData.goalBattle && curOwOs >= recruitData.goalOwO) {
                bot.log {
                    title = "${lxvUser.username}'s Recruitment"
                    description =
                        "${user.mention}'s recruitment expired, but they have enough stats already! They'll get <@&${LXV_MEMBER_ROLE_ID}> role when they run the command next"
                    color = LXVBot.LXV_PINK
                }
            } else {
                val battlesShort = max(0, recruitData.goalBattle - curBattles)
                val owosShort = max(0, recruitData.goalOwO - curOwOs)
                bot.client.rest.channel.createMessage(reminder.channelId) {
                    content = "${user.mention}, your recruitment expired :c"
                    embed {
                        description =
                            "${user.mention} was $owosShort OwO${if (owosShort != 1L) "s" else ""} short and $battlesShort battle${if (battlesShort != 1L) "s" else ""} short of becoming an LXV Member ${LXVBot.LXV_CRY_EMOJI.mention}"
                        field {
                            name = "Needed Stats"
                            value = "OwOs: ${recruitData.goalOwO}\n" +
                                    "Battles: ${recruitData.goalBattle}"
                            inline = true
                        }
                        field {
                            name = "Final Stats"
                            value = "OwOs: $curOwOs\n" +
                                    "Battles: $curBattles"
                            inline = true
                        }
                        color = LXVBot.LXV_MAGENTA
                    }
                }
                lxvUserCol.updateOne(
                    LXVUser::_id eq userId,
                    setValue(LXVUser::serverData / ServerData::recruitData / RecruitData::active, false)
                )
                MemberBehavior(LXVBot.LXV_GUILD_ID, userId, bot.client).removeRole(
                    LXVBot.LXV_RECRUIT_ROLE_ID,
                    "Recruitment Expired"
                )
            }
        }
    }

//    private val RECRUIT_CHANNEL_ID = Snowflake(829351473506287626) // staff spam
    private val RECRUIT_CHANNEL_ID = Snowflake(769134295360077824) // real recruit channel

    private val LXV_MEMBER_ROLE_ID = Snowflake(767034360640700427)
    private val LXV_RECRUIT_ROLE_ID = Snowflake(769135734086959104)

    private var OWOS_NEEDED = 2000
    private var BATTLES_NEEDED = 1800

//    private var OWOS_NEEDED = 1
//    private var BATTLES_NEEDED = 1


//    private var TIME_LIMIT_MS = 30L * 1000L
    private var TIME_LIMIT_MS = 30L * 24L * 60L * 60L * 1000L

}