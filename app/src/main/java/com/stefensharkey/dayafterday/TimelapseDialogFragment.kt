package com.stefensharkey.dayafterday

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.dialog_timelapse.*

class TimelapseDialogFragment : AppCompatDialogFragment() {

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

        customView.apply {
            findViewById<RadioGroup>(timelapse_resolution_radio.id).apply {
                for (x in Resolution.values().indices) {
                    addView(
                        RadioButton(context).apply {
                            text = resources.getStringArray(R.array.resolutions_array)[x]
                        }
                    )
                }
            }
        }

        val displayedValues = (IntArray(100) { i -> i + 1 }).map(Int::toString).toTypedArray()

        timelapse_frames_per_second.apply {
            minValue = 1
            maxValue = displayedValues.size - 1
            value = properties[getString(R.string.frames_per_second)] as Int
            this.displayedValues = displayedValues

            setOnValueChangedListener { _, _, newVal ->
                properties[getString(R.string.frames_per_second)] = newVal
            }
        }

        timelapse_open_when_finished.setOnCheckedChangeListener { _, isChecked ->
            properties[getString(R.string.open_when_finished)] = isChecked
        }

        timelapse_resolution_radio.apply {
            check((properties[getString(R.string.resolution)] as Resolution).ordinal + 1)

            setOnCheckedChangeListener { _, checkedId ->
                properties[getString(R.string.resolution)] = Resolution.values()[checkedId - 1]
            }
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
}