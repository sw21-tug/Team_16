package team16.easytracker

import android.content.ContentValues
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import team16.easytracker.database.Contracts.Company as CompanyContract
import team16.easytracker.database.Contracts.Worker as WorkerContract
import team16.easytracker.database.DbHelper
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class DbHelperTests {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val dbHelper = DbHelper(appContext)

    /** NEVER use setTransactionSuccessful() */
    val writableDb = dbHelper.writableDatabase
    val readableDb = dbHelper.readableDatabase

    val COMPANY_DUMMY_NAME = "company 1"
    val WORKER_DUMMY_FIRST_NAME = "Max"
    val WORKER_DUMMY_LAST_NAME = "Mustermann"
    val DUMMY_EMAIL = "test.test@test.at"
    val DUMMY_PASSWORD = "securePassword"

    @Before
    fun init() {
        writableDb.beginTransaction()
    }

    @After
    fun tearDown() {
        writableDb.endTransaction()
    }

    private fun insertDummyCompany(): Long {
        val values = ContentValues().apply {
            put(CompanyContract.COL_NAME, COMPANY_DUMMY_NAME)
            put(CompanyContract.COL_ADDRESS_ID, 1)
        }

        return writableDb.insert(CompanyContract.TABLE_NAME, null, values)
    }

    private fun insertDummyWorker(): Long {

        val values = ContentValues().apply {
            put(WorkerContract.COL_FIRST_NAME, WORKER_DUMMY_FIRST_NAME)
            put(WorkerContract.COL_LAST_NAME, WORKER_DUMMY_LAST_NAME)
            //put(Worker.COL_DATE_OF_BIRTH, "1998-05-10")
            put(WorkerContract.COL_TITLE, "")
            put(WorkerContract.COL_EMAIL, "max.mustermann@gmail.com")
            put(WorkerContract.COL_PASSWORD, "1235165465")
            put(WorkerContract.COL_PHONE_NUMBER, "+43 9509579554")
            //put(Worker.COL_CREATED_AT, DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            put(WorkerContract.COL_ADDRESS_ID, 1)
        }

        return writableDb.insert(WorkerContract.TABLE_NAME, null, values)
    }

    @Test
    fun testInsertCompany() {
        val newRowId = insertDummyCompany()
        assert(newRowId > 0)
    }

    @Test
    fun testInsertWorker() {
        val newRowId = insertDummyWorker()
        assert(newRowId > 0)
    }

    @Test
    fun testReadCompany() {
        val newRowId = insertDummyCompany()
        val query =
            "SELECT * FROM ${CompanyContract.TABLE_NAME} WHERE ${CompanyContract.COL_ID} = ?"
        val result = readableDb.rawQuery(query, arrayOf(newRowId.toString()))
        result.moveToFirst()
        val companyName = result.getString(result.getColumnIndexOrThrow(CompanyContract.COL_NAME))
        assert(companyName == COMPANY_DUMMY_NAME)
    }

    @Test
    fun testReadWorker() {
        val newRowId = insertDummyWorker()
        val query = "SELECT * FROM ${WorkerContract.TABLE_NAME} WHERE ${WorkerContract.COL_ID} = ?"
        val result = readableDb.rawQuery(query, arrayOf(newRowId.toString()))
        result.moveToFirst()
        val workerFirstName =
            result.getString(result.getColumnIndexOrThrow(WorkerContract.COL_FIRST_NAME))
        assert(workerFirstName == WORKER_DUMMY_FIRST_NAME)
        val workerLastName =
            result.getString(result.getColumnIndexOrThrow(WorkerContract.COL_LAST_NAME))
        assert(workerLastName == WORKER_DUMMY_LAST_NAME)
    }

    @Test
    fun testExecuteSQLScript() {
        val assetManager = appContext.assets
        val inputStream = assetManager.open("dbtest1.sql")
        dbHelper.executeSQLFile(writableDb, inputStream)
        val newRowId = insertDummyCompany()
        val query =
            "SELECT * FROM ${CompanyContract.TABLE_NAME} WHERE ${CompanyContract.COL_ID} = ?"
        val result = readableDb.rawQuery(query, arrayOf(newRowId.toString()))
        result.moveToFirst()
        val columnIndex = result.getColumnIndex("test1")
        assert(columnIndex != -1)
    }

    @Test
    fun testCompanyModel() {
        val dummyAddressId = 69;
        val id = dbHelper.saveCompany(COMPANY_DUMMY_NAME, dummyAddressId)
        val company = dbHelper.loadCompany(id)
        assert(company != null)
        assert(company?.name == COMPANY_DUMMY_NAME)
        assert(company?.addressId == dummyAddressId)
    }

    @Test
    fun testWorkerModel() {
        val now = LocalDateTime.now().withNano(0)
        val id = dbHelper.saveWorker(
            WORKER_DUMMY_FIRST_NAME,
            WORKER_DUMMY_LAST_NAME,
            now.toLocalDate(),
            "",
            DUMMY_EMAIL,
            DUMMY_PASSWORD,
            "",
            now,
            1
        )
        val worker = dbHelper.loadWorker(id)
        assert(worker != null)
        assert(worker?.firstName == WORKER_DUMMY_FIRST_NAME)
        assert(worker?.dateOfBirth == now.toLocalDate())
        assert(worker?.createdAt == now)
    }

    @Test
    fun testAddressModel() {
        val dummyStreet = "TEST"
        val dummyZipCode = "1234"
        val dummyCity = "Graz"
        val id = dbHelper.saveAddress(dummyStreet, dummyZipCode, dummyCity)
        val address = dbHelper.loadAddress(id)
        assert(address != null)
        assert(address?.street == dummyStreet)
        assert(address?.zipCode == dummyZipCode)
        assert(address?.city == dummyCity)
    }

    @Test
    fun testTrackingModel() {
        val dummyName = "TEST"
        val now = LocalDateTime.now()
        val id = dbHelper.saveTracking(dummyName, 1, now, now, "desc", "bluetooth")
        val tracking = dbHelper.loadTracking(id)
        assert(tracking != null)
        assert(tracking?.name == dummyName)
    }

    @Test
    fun testLogin() {
        val id = dbHelper.saveWorker(
            WORKER_DUMMY_FIRST_NAME,
            WORKER_DUMMY_LAST_NAME,
            LocalDate.now(),
            "",
            DUMMY_EMAIL,
            DUMMY_PASSWORD,
            "",
            LocalDateTime.now(),
            1
        )
        val worker = dbHelper.loginWorker(DUMMY_EMAIL, DUMMY_PASSWORD)
        assert(worker != null)
        assert(worker?.getId() == id)
    }

    @Test
    fun testAddWorkerToCompany() {
        val workerId = insertDummyWorker().toInt()
        val companyId = insertDummyCompany().toInt()
        val success = dbHelper.addWorkerToCompany(workerId, companyId, "pos")
        assert(success)

        // invalid workerId should throw
        var exceptionThrown = false
        try {
            dbHelper.addWorkerToCompany(-1, companyId, "pos")
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assert(exceptionThrown)

        // invalid companyId should throw
        exceptionThrown = false
        try {
            dbHelper.addWorkerToCompany(workerId, -1, "pos")
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assert(exceptionThrown)

        // double insert shouldn't break
        val doubleInsertSuccess = dbHelper.addWorkerToCompany(workerId, companyId, "pos")
        assert(doubleInsertSuccess)
    }
}