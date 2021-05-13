package it.polito.s279434.fragapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener


/**
 * A simple [Fragment] subclass.
 * Use the [InputFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InputFragment : Fragment(R.layout.fragment_input) {

    interface InputManager{
        fun onValue(value: String)
    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_input, container, false)
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editText = view.findViewById<EditText>(R.id.value)
        val button = view.findViewById<Button>(R.id.submit)
        button.isEnabled = false
        //TextWatcher is an interface
        //object implements (Anonymusly) the interface.
        val watcher = object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                button.isEnabled = s.toString().length >= 3
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        editText.addTextChangedListener( watcher)

        button.setOnClickListener {
            //Toast.makeText(activity, editText.text.toString(), Toast.LENGTH_LONG).show()
//            if (activity is InputManager){
//                (activity as InputManager).onValue(editText.text.toString())
//            }
            (activity as? InputManager)?.onValue(editText.text.toString())
        }
    }

}