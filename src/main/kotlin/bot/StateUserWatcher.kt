package bot

import bot.models.BotState
import dev.inmo.tgbotapi.types.Identifier
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StateUserWatcher(val stateUser: MutableMap<Identifier, BotState>) {
    val timeStartStateUser: MutableMap<Identifier, DateTime> = mutableMapOf()
    val expiredTimeMinutes: Int = 1

    suspend fun start(){
        CoroutineScope(Dispatchers.Default).launch {
            stateUser.forEach { (identifier, botState) -> //проверяем на новых и просроченных
                if (timeStartStateUser.containsKey(identifier)){
                    //проверка на просроченность
                } else {
                    //добавляем нового
                    timeStartStateUser[identifier] = DateTime.now()
                }
            }

                // тут проверка на удаленных ии очистка
                        
        }.start()
    }
}