package artemis.agent.generic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.databinding.GenericDataEntryBinding

class GenericDataViewHolder private constructor(
    private val entryBinding: GenericDataEntryBinding
) : RecyclerView.ViewHolder(entryBinding.root) {
    var name: CharSequence?
        get() = entryBinding.entryNameLabel.text
        set(text) { entryBinding.entryNameLabel.text = text }

    var data: CharSequence?
        get() = entryBinding.entryDataLabel.text
        set(text) {
            entryBinding.entryDataLabel.text = text
            entryBinding.entryDataLabel.visibility = if (text.isNullOrBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

    constructor(parent: ViewGroup) : this(
        GenericDataEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    fun bind(entry: GenericDataEntry) {
        name = entry.name
        data = entry.data
    }
}
