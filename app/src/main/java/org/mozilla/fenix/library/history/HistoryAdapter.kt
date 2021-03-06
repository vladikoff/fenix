/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import org.mozilla.fenix.R
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.history_delete.view.*
import kotlinx.android.synthetic.main.history_header.view.*
import kotlinx.android.synthetic.main.history_list_item.view.*
import mozilla.components.browser.menu.BrowserMenu

class HistoryAdapter(
    private val actionEmitter: Observer<HistoryAction>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class HistoryListItemViewHolder(
        view: View,
        private val actionEmitter: Observer<HistoryAction>
    ) : RecyclerView.ViewHolder(view) {

        private val checkbox = view.should_remove_checkbox
        private val favicon = view.history_favicon
        private val title = view.history_title
        private val url = view.history_url
        private val menuButton = view.history_item_overflow

        private var item: HistoryItem? = null
        private lateinit var historyMenu: HistoryItemMenu
        private var mode: HistoryState.Mode = HistoryState.Mode.Normal
        private val checkListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (mode is HistoryState.Mode.Normal) {
                return@OnCheckedChangeListener
            }

            item?.apply {
                val action = if (isChecked) {
                    HistoryAction.AddItemForRemoval(this)
                } else {
                    HistoryAction.RemoveItemForRemoval(this)
                }

                actionEmitter.onNext(action)
            }
        }

        init {
            setupMenu()

            view.setOnClickListener {
                if (mode is HistoryState.Mode.Editing) {
                    checkbox.isChecked = !checkbox.isChecked
                    return@setOnClickListener
                }

                item?.apply {
                    actionEmitter.onNext(HistoryAction.Select(this))
                }
            }

            view.setOnLongClickListener {
                item?.apply {
                    actionEmitter.onNext(HistoryAction.EnterEditMode(this))
                }

                true
            }

            menuButton.setOnClickListener {
                historyMenu.menuBuilder.build(view.context).show(
                    anchor = it,
                    orientation = BrowserMenu.Orientation.DOWN)
            }

            checkbox.setOnCheckedChangeListener(checkListener)
        }

        fun bind(item: HistoryItem, mode: HistoryState.Mode) {
            this.item = item
            this.mode = mode

            title.text = item.title
            url.text = item.url

            val isEditing = mode is HistoryState.Mode.Editing
            checkbox.visibility = if (isEditing) { View.VISIBLE } else { View.GONE }
            favicon.visibility = if (isEditing) { View.INVISIBLE } else { View.VISIBLE }

            if (mode is HistoryState.Mode.Editing) {
                checkbox.setOnCheckedChangeListener(null)

                // Don't set the checkbox if it already contains the right value.
                // This prevent us from cutting off the animation
                val shouldCheck = mode.selectedItems.contains(item)
                if (checkbox.isChecked != shouldCheck) {
                    checkbox.isChecked = mode.selectedItems.contains(item)
                }
                checkbox.setOnCheckedChangeListener(checkListener)
            }
        }

        private fun setupMenu() {
            this.historyMenu = HistoryItemMenu(itemView.context) {
                when (it) {
                    is HistoryItemMenu.Item.Delete -> {
                        item?.apply { actionEmitter.onNext(HistoryAction.Delete.One(this)) }
                    }
                }
            }
        }

        companion object {
            const val LAYOUT_ID = R.layout.history_list_item
        }
    }

    class HistoryHeaderViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        private val title = view.history_header_title

        fun bind(title: String) {
            this.title.text = title
        }

        companion object {
            const val LAYOUT_ID = R.layout.history_header
        }
    }

    class HistoryDeleteViewHolder(
        view: View,
        private val actionEmitter: Observer<HistoryAction>
    ) : RecyclerView.ViewHolder(view) {
        private lateinit var mode: HistoryState.Mode

        private val button = view.delete_history_button.apply {
            setOnClickListener {
                val mode = mode
                if (mode is HistoryState.Mode.Editing && mode.selectedItems.isNotEmpty()) {
                    actionEmitter.onNext(HistoryAction.Delete.Some(mode.selectedItems))
                } else {
                    actionEmitter.onNext(HistoryAction.Delete.All)
                }
            }
        }

        private val text = view.delete_history_button_text.apply {
            val color = ContextCompat.getColor(context, R.color.photonRed60)
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_delete)
            drawable?.setTint(color)
            this.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }

        fun bind(mode: HistoryState.Mode) {
            this.mode = mode

            val text = if (mode is HistoryState.Mode.Editing && mode.selectedItems.isNotEmpty()) {
                text.context.resources.getString(R.string.history_delete_some, mode.selectedItems.size)
            } else {
                text.context.resources.getString(R.string.history_delete_all)
            }

            button.contentDescription = text
            this.text.text = text
        }

        companion object {
            const val LAYOUT_ID = R.layout.history_delete
        }
    }

    private var items: List<HistoryItem> = emptyList()
    private var mode: HistoryState.Mode = HistoryState.Mode.Normal

    fun updateData(items: List<HistoryItem>, mode: HistoryState.Mode) {
        this.items = items
        this.mode = mode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            HistoryDeleteViewHolder.LAYOUT_ID -> HistoryDeleteViewHolder(view, actionEmitter)
            HistoryHeaderViewHolder.LAYOUT_ID -> HistoryHeaderViewHolder(view)
            HistoryListItemViewHolder.LAYOUT_ID -> HistoryListItemViewHolder(view, actionEmitter)
            else -> throw IllegalStateException("viewType $viewType does not match to a ViewHolder")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> HistoryDeleteViewHolder.LAYOUT_ID
            1 -> HistoryHeaderViewHolder.LAYOUT_ID
            else -> HistoryListItemViewHolder.LAYOUT_ID
        }
    }

    override fun getItemCount(): Int = items.count() + NUMBER_OF_SECTIONS

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HistoryDeleteViewHolder -> holder.bind(mode)
            // Temporarily hard code the header until
            // we build out support for time based sections
            is HistoryHeaderViewHolder -> holder.bind("Today")
            is HistoryListItemViewHolder -> holder.bind(items[position - NUMBER_OF_SECTIONS], mode)
        }
    }

    companion object {
        // Temporarily hard code the number of sections until
        // we build out support for time based sections
        private const val NUMBER_OF_SECTIONS = 2
    }
}
