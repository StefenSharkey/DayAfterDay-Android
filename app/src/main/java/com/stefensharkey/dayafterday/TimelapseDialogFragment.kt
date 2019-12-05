package com.stefensharkey.dayafterday

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.dialog_timelapse.*

class TimelapseDialogFragment : AppCompatDialogFragment() {

    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener

    private lateinit var customView: View

    var framesPerSecond = 10
    var openWhenFinished = false

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialogFragment: AppCompatDialogFragment)
        fun onDialogNegativeClick(dialogFragment: AppCompatDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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

        val displayedValues = (IntArray(100) {i -> i + 1}).map(Int::toString).toTypedArray()

        timelapse_frames_per_second.minValue = 1
        timelapse_frames_per_second.maxValue = displayedValues.size - 1
        timelapse_frames_per_second.value = framesPerSecond
        timelapse_frames_per_second.displayedValues = displayedValues

        timelapse_frames_per_second.setOnValueChangedListener { _, _, newVal ->
            framesPerSecond = newVal
        }

        timelapse_open_when_finished.setOnCheckedChangeListener { _, isChecked ->
            openWhenFinished = isChecked
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