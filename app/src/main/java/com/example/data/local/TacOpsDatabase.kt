package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.TacOpsDao
import com.example.data.local.model.InventoryItemEntity
import com.example.data.local.model.OperatorEntity
import com.example.data.local.model.TacticalLogEntity
import com.example.data.local.model.TacticalZoneEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        OperatorEntity::class,
        InventoryItemEntity::class,
        TacticalZoneEntity::class,
        TacticalLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TacOpsDatabase : RoomDatabase() {
    abstract fun tacOpsDao(): TacOpsDao

    companion object {
        @Volatile
        private var INSTANCE: TacOpsDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TacOpsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TacOpsDatabase::class.java,
                    "tacops_airsoft.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialTacticalData(database.tacOpsDao())
                    }
                }
            }
        }

        suspend fun populateInitialTacticalData(dao: TacOpsDao) {
            // Base reference point (A typical outdoor airsoft field: -23.550520, -46.633308)
            val baseLat = -23.550520
            val baseLng = -46.633308

            val op1 = OperatorEntity(
                callsign = "Ghost",
                realName = "Gabriel Silva",
                role = "Comandante",
                squad = "Alpha",
                healthState = "ACTIVE",
                hpPercent = 100,
                hitsTaken = 0,
                latitude = baseLat,
                longitude = baseLng,
                heading = 45f,
                radioFrequency = "CH 01 - 462.562 MHz",
                batteryPercent = 98,
                isUserDevice = true
            )
            val op2 = OperatorEntity(
                callsign = "Viper",
                realName = "Marcos Prado",
                role = "Sniper / DMR",
                squad = "Alpha",
                healthState = "ACTIVE",
                hpPercent = 100,
                hitsTaken = 0,
                latitude = baseLat + 0.00065,
                longitude = baseLng + 0.00045,
                heading = 120f,
                radioFrequency = "CH 01 - 462.562 MHz",
                batteryPercent = 89
            )
            val op3 = OperatorEntity(
                callsign = "Spectre",
                realName = "Lucas Andrade",
                role = "Batedor / Recon",
                squad = "Alpha",
                healthState = "COMBAT",
                hpPercent = 75,
                hitsTaken = 1,
                latitude = baseLat - 0.00040,
                longitude = baseLng + 0.00080,
                heading = 270f,
                radioFrequency = "CH 01 - 462.562 MHz",
                batteryPercent = 74
            )
            val op4 = OperatorEntity(
                callsign = "Reaper",
                realName = "Rodrigo Ferreira",
                role = "Suporte / Gunner",
                squad = "Bravo",
                healthState = "ACTIVE",
                hpPercent = 100,
                hitsTaken = 0,
                latitude = baseLat - 0.00075,
                longitude = baseLng - 0.00050,
                heading = 10f,
                radioFrequency = "CH 02 - 462.587 MHz",
                batteryPercent = 92
            )
            val op5 = OperatorEntity(
                callsign = "Phoenix",
                realName = "Dra. Juliana Mendes",
                role = "Médico",
                squad = "Bravo",
                healthState = "ACTIVE",
                hpPercent = 100,
                hitsTaken = 0,
                latitude = baseLat + 0.00030,
                longitude = baseLng - 0.00060,
                heading = 180f,
                radioFrequency = "CH 02 - 462.587 MHz",
                batteryPercent = 85
            )

            val op1Id = dao.insertOperator(op1)
            val op2Id = dao.insertOperator(op2)
            val op3Id = dao.insertOperator(op3)
            val op4Id = dao.insertOperator(op4)
            val op5Id = dao.insertOperator(op5)

            // Insert initial inventory items for Ghost
            dao.insertInventoryItems(
                listOf(
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "PRIMARY",
                        name = "MK18 Mod 1 AEG Full Metal",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "390 FPS (0.20g) | Hop-up Maple Leaf 60°",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "AMMO",
                        name = "Mags Mid-Cap PTS EPM (150 BBs)",
                        quantity = 5,
                        maxQuantity = 6,
                        details = "BBs 0.28g Bio BLS | 750 BBs total",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "SECONDARY",
                        name = "Glock 17 Gen 5 GBB Umarex",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "310 FPS | Green Gas Nimrod",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "GEAR",
                        name = "Colete Plate Carrier JPC Multicam",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "Placas Dummy EVA | Porta Mag Triplo",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "TACTICAL",
                        name = "Rádio Baofeng UV-5R + PTT Tático",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "Freq 462.562 | Headset Z-Tac Bowman",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "TACTICAL",
                        name = "Granada de Fumaça M18 (Verde)",
                        quantity = 2,
                        maxQuantity = 3,
                        details = "Efeito cortina 60s",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op1Id,
                        category = "MEDICAL",
                        name = "Faixa de Respawn / Pano Vermelho (Dead Rag)",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "Sinalização de baixa obrigatória",
                        isEquipped = true
                    )
                )
            )

            // Insert initial inventory for Viper (Sniper)
            dao.insertInventoryItems(
                listOf(
                    InventoryItemEntity(
                        operatorId = op2Id,
                        category = "PRIMARY",
                        name = "Tokyo Marui VSR-10 G-Spec Upgraded",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "500 FPS (0.20g) | BBs 0.43g Geoffs | Luneta 3-9x40",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op2Id,
                        category = "SECONDARY",
                        name = "MK23 Socom Stealth NBB",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "320 FPS com Silenciador",
                        isEquipped = true
                    ),
                    InventoryItemEntity(
                        operatorId = op2Id,
                        category = "GEAR",
                        name = "Ghillie Suit Sniper Viper Hood",
                        quantity = 1,
                        maxQuantity = 1,
                        details = "Camuflagem Woodland",
                        isEquipped = true
                    )
                )
            )

            // Default Exclusion Zone (Área de Risco / Pedreira - Fora de Jogo)
            // Polygon around baseLat + 0.0005, baseLng + 0.0005
            val exclusionPoints = """
                [
                    {"lat": ${baseLat + 0.00050}, "lng": ${baseLng + 0.00030}},
                    {"lat": ${baseLat + 0.00090}, "lng": ${baseLng + 0.00040}},
                    {"lat": ${baseLat + 0.00085}, "lng": ${baseLng + 0.00080}},
                    {"lat": ${baseLat + 0.00040}, "lng": ${baseLng + 0.00070}}
                ]
            """.trimIndent().replace("\n", "").replace(" ", "")

            dao.insertZone(
                TacticalZoneEntity(
                    name = "Zona de Exclusão - Pedreira",
                    zoneType = "EXCLUSION",
                    pointsJson = exclusionPoints,
                    baseColorHex = "#FF1744",
                    isBreached = false
                )
            )

            // Default Strategic Objective (Bunker Central - Ponto Alfa)
            val objectivePoints = """
                [
                    {"lat": ${baseLat - 0.00020}, "lng": ${baseLng - 0.00020}},
                    {"lat": ${baseLat - 0.00020}, "lng": ${baseLng + 0.00020}},
                    {"lat": ${baseLat - 0.00055}, "lng": ${baseLng + 0.00025}},
                    {"lat": ${baseLat - 0.00060}, "lng": ${baseLng - 0.00025}}
                ]
            """.trimIndent().replace("\n", "").replace(" ", "")

            dao.insertZone(
                TacticalZoneEntity(
                    name = "Objetivo Estratégico - Bunker Alfa",
                    zoneType = "STRATEGIC_OBJECTIVE",
                    pointsJson = objectivePoints,
                    baseColorHex = "#00E676",
                    isBreached = false
                )
            )

            // Initial logs
            dao.insertLog(
                TacticalLogEntity(
                    operatorCallsign = "SISTEMA",
                    eventType = "QR_SYNC",
                    message = "Operação 'Falcão Negro' inicializada via QR Code de comando.",
                    severity = "SUCCESS"
                )
            )
            dao.insertLog(
                TacticalLogEntity(
                    operatorCallsign = "Ghost",
                    eventType = "STATUS_CHANGE",
                    message = "Esquadrão Alpha em posição no grid tático.",
                    severity = "INFO"
                )
            )
        }
    }
}
