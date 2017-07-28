package org.jdc.template.inject

import android.app.Application
import android.app.NotificationManager
import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.gms.analytics.GoogleAnalytics
import dagger.Module
import dagger.Provides
import org.jdc.template.Analytics
import org.jdc.template.BuildConfig
import org.jdc.template.BusRegistry
import org.jdc.template.datasource.database.main.MainDatabase
import org.jdc.template.datasource.database.main.household.HouseholdDao
import org.jdc.template.datasource.database.main.individual.IndividualDao
import org.jdc.template.datasource.webservice.ServiceModule
import org.jdc.template.json.DateTimeStringDeserializer
import org.jdc.template.json.DateTimeStringSerializer
import org.jdc.template.util.CoroutineContextProvider
import org.threeten.bp.LocalDateTime
import pocketbus.Bus
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Singleton

@Module(includes = arrayOf(ServiceModule::class))
class AppModule(private val application: Application) {

    @Provides
    @Singleton
    internal fun provideApplication(): Application {
        return application
    }

    @Provides
    fun provideSharedPreferences(application: Application): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application)
    }

    @Provides
    fun provideNotificationManager(application: Application): NotificationManager {
        return application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideAnalytics(): Analytics {
        // Only send analytics to Google Analytics with versions of the app that are NOT debuggable (such as BETA or RELEASE)
        if (BuildConfig.DEBUG) {
            return object : Analytics {
                override fun send(params: Map<String, String>) {
                    Log.d("Analytics", params.toString())
                }
            }

        }

        val googleAnalytics = GoogleAnalytics.getInstance(application)
        val tracker = googleAnalytics.newTracker(BuildConfig.ANALYTICS_KEY)
        // tracker.setSessionTimeout(300); // default is 30 seconds
        return Analytics.GoogleAnalytics(tracker)
    }

    @Provides
    @Singleton
    internal fun provideEventBus(): Bus {
        val bus = Bus.Builder()
                .build()
        bus.setRegistry(BusRegistry())
        return bus
    }

    @Provides
    @Singleton
    fun provideObjectMapper(): ObjectMapper {
        val module = SimpleModule("Jackson MODULE")
        module.addSerializer(LocalDateTime::class.java, DateTimeStringSerializer())
                .addDeserializer(LocalDateTime::class.java, DateTimeStringDeserializer())

        val mapper = jacksonObjectMapper()
        mapper.registerModule(module)

        return mapper
    }

    @Provides
    @Singleton
    fun provideJacksonConverterFactory(objectMapper: ObjectMapper): JacksonConverterFactory {
        return JacksonConverterFactory.create(objectMapper)
    }

    @Provides
    @Singleton
    fun provideCoroutineContextProvider(): CoroutineContextProvider {
        return CoroutineContextProvider.MainCoroutineContextProvider
    }

    @Provides
    @Singleton
    fun provideMainDatabase(application: Application): MainDatabase {
        return Room.databaseBuilder(application, MainDatabase::class.java, MainDatabase.DATABASE_NAME)
//                .addMigrations(object: Migration(1, 2) {
//                    override fun migrate(p0: SupportSQLiteDatabase?) {
//                    }
//                })
                .build()
    }

    @Provides
    @Singleton
    fun provideIndividualDao(mainDatabase: MainDatabase): IndividualDao {
        return mainDatabase.individualDao()
    }

    @Provides
    @Singleton
    fun provideHouseholdDao(mainDatabase: MainDatabase): HouseholdDao {
        return mainDatabase.householdDao()
    }
}