package it.polito.s279434.list

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val boys = listOf<String>("JohnJohnJohnJohnJohn", "Mark", "Tom", "John", "Mark", "Tom", "John", "Mark", "Tom", "John").map { it -> MalePerson(it) }
        val girls = listOf<String>("Kate", "Sabrina", "Veronica","Kate", "Sabrina", "Veronica", "Kate", "Sabrina", "Veronica").map { it -> FemalePerson(it) }
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val people = boys.zip(girls).flatMap { it -> listOf(it.first, it.second) }
        recyclerView.adapter = PersonAdapter(people.shuffled())
    }
}

open class Person(val name:String){
}

class MalePerson(name: String): Person(name){

}

class FemalePerson(name: String): Person(name){

}

class PersonAdapter(val data:List<Person>): RecyclerView.Adapter<PersonAdapter.PersonViewHolder>(){

    class PersonViewHolder(v: View): RecyclerView.ViewHolder(v){
        val nameView = v.findViewById<TextView>(R.id.textView)
        val button = v.findViewById<Button>(R.id.button)

        fun bind(p: Person){
            nameView.text = p.name
            button.setOnClickListener { Toast.makeText(nameView.context, p.name, Toast.LENGTH_LONG).show() }
        }

        fun unbind(){
            button.setOnClickListener { null }
        }
    }

    override fun onViewRecycled(holder: PersonViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = if(viewType == R.layout.male_layout){
            layoutInflater.inflate(R.layout.male_layout, parent, false)
        } else{
            layoutInflater.inflate(R.layout.female_layout, parent, false)
        }

        return PersonViewHolder(layout)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int{
        if (data[position] is MalePerson)
            return R.layout.male_layout
        else
            return R.layout.female_layout
    }

}