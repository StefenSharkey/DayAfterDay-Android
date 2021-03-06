package com.stefensharkey.dayafterday

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.dialog_timelapse.*

class TimelapseDialogFragment : AppCompatDialogFragment(), AdapterView.OnItemSelectedListener {

    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener

    private lateinit var customView: View

    lateinit var properties: HashMap<String, Any>

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialogFragment: AppCompatDialogFragment)
        fun onDialogNegativeClick(dialogFragment: AppCompatDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        properties = hashMapOf(
            Pair(getString(R.string.frames_per_second), 10),
            Pair(getString(R.string.resolution), Resolution.RES_1080P),
            Pair(getString(R.string.encoding_format), EncodingFormat.X264),
            Pair(getString(R.string.open_when_finished), false)
        )

        customView = (activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.dialog_timelapse, null)

        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.timelapse_settings)
            .setView(customView)
            .setPositiveButton(R.string.ok) { _, _ -> listener.onDialogPositiveClick(this) }
            .setNegativeButton(R.string.cancel) { _, _ -> listener.onDialogNegativeClick(this) }

        return builder.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return customView
    }

    override fun onResume() {
        super.onResume()

        // Frames per second NumberPicker
        timelapse_frames_per_second.apply {
            val displayedValues = (IntArray(100) { i -> i + 1 }).map(Int::toString).toTypedArray()

            minValue = 1
            maxValue = displayedValues.size - 1
            value = properties[getString(R.string.frames_per_second)] as Int
            this.displayedValues = displayedValues

            setOnValueChangedListener { _, _, newVal ->
                properties[getString(R.string.frames_per_second)] = newVal
            }
        }

        // Resolutions Spinner
        // Fill spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.resolutions_array,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timelapse_resolution_spinner.adapter = adapter
        }

        timelapse_resolution_spinner.also { spinner ->
            spinner.setSelection((properties[getString(R.string.resolution)] as Resolution).ordinal)
            spinner.onItemSelectedListener = this
        }

        // Encoding Format Spinner
        // Fill spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.encoding_formats_array,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timelapse_encoding_format_spinner.adapter = adapter
        }

        timelapse_encoding_format_spinner.also { spinner ->
            spinner.setSelection((properties[getString(R.string.encoding_format)] as EncodingFormat).ordinal)
            spinner.onItemSelectedListener = this
        }

        // Open when finished CheckBox
        timelapse_open_when_finished.setOnCheckedChangeListener { _, isChecked ->
            properties[getString(R.string.open_when_finished)] = isChecked
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface.
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception.
            throw ClassCastException(("$context must implement NoticeDialogListener."))
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.timelapse_resolution_spinner -> properties[getString(R.string.resolution)] = Resolution.values()[position]
            R.id.timelapse_encoding_format_spinner -> properties[getString(R.string.encoding_format)] = EncodingFormat.values()[position]
        }
    }
}