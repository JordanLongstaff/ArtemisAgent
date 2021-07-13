package artemis.agent.generic

import androidx.recyclerview.widget.DiffUtil

class GenericDataDiffUtilCallback(
    private val oldList: List<GenericDataEntry>,
    private val newList: List<GenericDataEntry>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = oldList[oldItemPosition]
        val newEntry = newList[newItemPosition]
        return newEntry == oldEntry
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true
}
