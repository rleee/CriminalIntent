# CriminalIntent
###### Chapter 8

#### Index:
- [Fragments](#fragments)
- [RecycleView](#recycleview)

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




