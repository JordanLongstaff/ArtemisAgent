package artemis.agent.generic

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class GenericDataAdapter(
    private val onClickItem: (GenericDataViewHolder) -> Unit = { },
) : RecyclerView.Adapter<GenericDataViewHolder>() {
    private var entries = listOf<GenericDataEntry>()

    override fun getItemCount(): Int = entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericDataViewHolder =
        GenericDataViewHolder(parent)

    override fun onBindViewHolder(holder: GenericDataViewHolder, position: Int) {
        holder.bind(entries[position])
        holder.itemView.setOnClickListener { onClickItem(holder) }
    }

    fun onListUpdate(list: List<GenericDataEntry>) {
        if (entries == list) return

        DiffUtil.calculateDiff(
            GenericDataDiffUtilCallback(entries, list)
        ).dispatchUpdatesTo(this)
        entries = list
    }
}
