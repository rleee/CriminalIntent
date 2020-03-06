# CriminalIntent
###### Chapter 8

#### Index:
- [Fragments](#fragments)
- [RecycleView](#recycleview)
- ConstraintLayout
- [Room database](#room-database)
- [Fragment Navigation](#fragment-navigation)

---
### Fragments

Fragment needs an Activity to be the host and it will be inflated in activity_main.xml FrameLayout
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        setContentView(R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment == null) {
            val fragment = CrimeListFragment.newInstance() // this instance is from companion object in Fragment class
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }
}
```

Fragment has lifecycle **similar** to Activity [credit: tutorialspoint.com](https://www.tutorialspoint.com/android/android_fragments.htm)

![tutorialspoint.com](https://www.tutorialspoint.com/android/images/fragment.jpg)

Difference between `onCreate(...)` and `onCreateView(...)`
- `onCreate(...)`
The system calls this when creating the fragment. Within your implementation, you should initialize essential components of the fragment that you want to retain when the fragment is paused or stopped, then resumed.

- `onCreateView(...)`
The system calls this when it's time for the fragment to draw its user interface for the first time. To draw a UI for your fragment, you must return a View from this method that is the root of your fragment's layout. You can return null if the fragment does not provide a UI.

Then we will have to inflate Fragment xml from `onCreateView(...)` and return that view
```kotlin
override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox

        dateButton.apply {
            text = crime.date.toString()
            isEnabled = false
        }
        
        return view
    }
```


---
### RecycleView
`implementation "androidx.recyclerview:recyclerview:x.x.x"`

Components needed to setting up RecyclerView:

xml
- list item xml
- recycler view xml

class
- Holder class
- Adapter class
- Host class

Initialize RecyclerView in host class, in this case it would be Fragment class `onCreateView(...)` method
```kotlin
crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
crimeRecyclerView.layoutManager = LinearLayoutManager(context)
```

Setup Holder class (this is the inner class in Fragment)
```kotlin
private inner class CrimeHolder(view: View): RecyclerView.ViewHolder(view) {

    private lateinit var crime: Crime

    private val titleTextView: TextView = view.findViewById(R.id.crime_title)
    private val dateTextView: TextView = view.findViewById(R.id.crime_date)

    fun bind(crime: Crime) {
        this.crime = crime
        this.titleTextView.text = this.crime.title
        this.dateTextView.text = this.crime.date.toString()
    }
}
```

Setup Adapter class (this is the inner class in Fragment)
```kotlin
private inner class CrimeAdapter(var crimes: List<Crime>): RecyclerView.Adapter<CrimeHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
        return CrimeHolder(view)
    }

    override fun getItemCount(): Int = crimes.size

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        val crime = crimes[position]
        holder.bind(crime)
    }
}
```

Wireup Adapter to RecyclerView in host class
```kotlin
val crimes = crimeListViewModel.crimes // crimes is List<Crime> in viewModel
adapter = CrimeAdapter(crimes)
crimeRecyclerView.adapter = adapter
```

---
### Room Database
```gradle
apply plugin: 'kotlin-kapt'
...
implementation 'androidx.room:room-runtime:x.x.x'
kapt 'androidx.room:room-compiler:x.x.x'
```

outline:
- [Entity](#entity) (from Model), this would be the table
- [TypeConverter](#typeconverter), to convert non primitive type to primitive type to store in database table
                 or to convert primitive type to non primitive type to let Model class use it
- [Dao](#dao-data-access-object) (Data Access Object), to query data from database
- [Database](#database)
- [Repository](#repository) *optional*, to function as a distributor to storing and fetching data
- [To use Room Instance](#to-use-room-instance)

  
>  
> Room: flow conclusion
>
> App Start  
> &nbsp;&nbsp;⤷ Application `onCreate()`  
> &nbsp;&nbsp;&nbsp;&nbsp;⤷ Create Repository `.initialize(getApplication())` & `INSTANCE`  
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;⤷ Create Database `.build()`  
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;⤷ ViewModel get Repository Instance `.get()`  

> if process death, the Repository `INSTANCE` & previous database `.build()` will still persist, only viewModel will re-`.get()` the `INSTANCE`

##### Entity
Update the Model to be a Room `@Entity` and assign id as `@PrimaryKey`, now this class would be a crime table schema
```kotlin
@Entity
data class Crime(@PrimaryKey val id: UUID = UUID.randomUUID(),
                 var title: String = "",
                 var date: Date = Date(),
                 var isSolved: Boolean = false) {
}
```

##### TypeConverter
Convert the non primitive type to primitive to store in database and vice versa, here date and id

```kotlin
class CrimeTypeConverters {

    // convert Date Object to record to database
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    // convert date from database to Date Object
    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }

    // convert data from database to UUID Object
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    // convert UUID Object to record to database
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }
}
```

##### Dao (Data Access Object)

as a query function to store and fetch data from database, this is an `inferface` Room will generate the concrete class internally to implements these function and query we declare.

```kotlin
@Dao
interface CrimeDao {

    @Query("SELECT * FROM crime")
    fun getCrimes(): LiveData<List<Crime>>

    @Query("SELECT * FROM crime WHERE id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>
}
```

##### Database
Then create Database class to represent the database and to hold the table (Model/Entity class), it is an `abstract` class so we cannot make an instance of it, but to pass it to database builder later (on repository) to build a database with the Dao functions we declare inside.

```kotlin
@Database(entities = [Crime::class], version = 1)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase: RoomDatabase() {

    abstract fun crimeDao(): CrimeDao
}
```

##### Repository

Here we use a singleton pattern repository, where in 1 app, only got 1 repository instance, and we use it on Application Context when the application initialized.

We use private constructor here to prevent others to instantiate the class, because this a regular class not abstract. so when need to use the class we have to call it like a static function `CrimeRepository.initialize(context)` and `CrimeRepository.get()`

Then we build the database and access the Dao to store and fetch data.

```kotlin
private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context){

    private val database: CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val crimeDao = database.crimeDao()

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    companion object {
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?:
                    throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}
```

#### To use Room Instance
Implementation here we want use a singleton on Application Context, so we make a class extending `Application()` and override the application `onCreate()` function to instantiate the Repository instance when we open the app and it runs the `onCreate()`

```kotlin
class CriminalIntentApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}
```

To use this class on Application Context, we will reference it on AndroidManifext.xml, then when we open the app, this class will be called with the overridden onCreate
```xml
<application
        ...
        android:name=".CriminalIntentApplication"
        ...
```

Then use the Repository on ViewModel, and observe the data in Fragment
```kotlin
class CrimeListViewModel: ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()
}
```

Data observation here we do it after the view has been created and ready with `override fun onViewCreated(...)`

But first when we initialize the recyclerView adapter we make it an empty list, the we fill it up in `onViewCreated(...)` with LiveData Observer.

When we Observe the data we pass in two parameters, viewLifecycleOwner and Observer.
- viewLifecycleOwner, the Observer will observe based on this viewLifeCycleOwner state, which means we will unsubscribe
                      from observer if the view is in invalid state / being torn down) or app will crash.
                      we use viewLifecycleOwner to keep track of the Fragment's view lifecycle (view = xml view?)
- Observer, to observe data changes and update the static List<Crime>


```kotlin
class CrimeListFragment: Fragment() {
    ...
    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())
    ...
    
    override fun onCreateView(...): View? {
        ...
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    Log.i("CrimeListFragment", "onViewCreated: got these many crime ${it.size}")
                    updateUI(it)
                }
            }
        )
    }
    
    private fun updateUI(crimes: List<Crime>) {
        adapter = CrimeAdapter(crimes)
        crimeRecyclerView.adapter = adapter
    }
}
```

### Fragment Navigation

To navigate from `CrimeListFragment` to `CrimeFragment`(details) we will have to go though the host activity to replace / launch the targeted fragment.

What we do here is declare a `interface` on `CrimeListFragment` to be implemented in host activity, if the list item is clicked the certain action will be run from host activity (here we launch `CrimeFragment`)

but we will have to attach the callback to the fragment first (`onAttach(...)`) and detach it once we left the fragment (`onDetach()`)

```kotlin
class CrimeListFragment: Fragment() {
    
    /**
     * Required interface for hosting activities
     */
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null
    
    ...
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }
    
    ...
    
    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }
}
```

Then we pass the crimeId through `onClick()` in `ViewHolder`

```kotlin
class CrimeListFragment: Fragment() {

    private inner class CrimeHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {
        
            ...
            
            override fun onClick(v: View?) {
                callbacks?.onCrimeSelected(crime.id)
            }
            
            ...
        
        }
}
```

After that from `MainActivity` we will implement the interface and replace the fragment,  
look closely from the code below we instantiate the `CrimeFragment` with the `companion object` we declare in that class,

why we did that is because: easier to maintain the code and we also use `fragment argument` to pass `crimeId` to `CrimeFragment`.
We also add this fragment replacement action to back stack and didn't name it anything / `null`

```kotlin
class MainActivity : AppCompatActivity(),
    CrimeListFragment.Callbacks {
    
    ...
    
    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
```

then the companion object in `CrimeFragment` to instantiate the `CrimeFragment` class and use `Fragment Argument` to pass bundled data, and from `onCreate` we will extract out the `crimeId` from `Fragment Argument`

```kotlin
private const val ARG_CRIME_ID = "crime_id"

class CrimeFragment: Fragment() {

    ...
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
    }
    
    ...

    companion object {
            fun newInstance(crimeId: UUID): CrimeFragment {
                val args = Bundle().apply {
                    putSerializable(ARG_CRIME_ID, crimeId)
                }
                return CrimeFragment().apply {
                    arguments = args
                }
            }
        }
}
```

Next we will make the `CrimeDetailViewModel` for `CrimeFragment` to fetch and store the data from Repository

We pass in the `crimeId` through `loadCrime()` And we are using `MutableLiveData` to store the `crimeId`, and `LiveData` to watch (`Transformations.switchMap`) the value in `MutableLive` data change and fetch new Crime Details from database

```kotlin
class CrimeDetailViewModel: ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    private val crimeIdLiveData = MutableLiveData<UUID>()

    var crimeLiveData: LiveData<Crime?> =
        Transformations.switchMap(crimeIdLiveData) { crimeId ->
            crimeRepository.getCrime(crimeId)
        }

    fun loadCrime(crimeId: UUID) {
        crimeIdLiveData.value = crimeId
    }
}
```

lastly implement the `CrimeDetailViewModel` to `CrimeFragment` and load the `crimeId` to ViewModel to start fetch the data, and `Observe` data changes at `onViewCreated(...)` to update the UI

```kotlin
class CrimeFragment: Fragment() {

    ...
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = it
                    updateUI()
                }
            }
        )
    }
    
    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
    }
}
```











