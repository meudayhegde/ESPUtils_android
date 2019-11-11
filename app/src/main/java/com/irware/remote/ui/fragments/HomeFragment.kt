package com.irware.remote.ui.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.irware.remote.MainActivity
import com.irware.remote.R
import com.irware.remote.holders.RemoteProperties
import com.irware.remote.ui.dialogs.RemoteDialog

class HomeFragment : androidx.fragment.app.Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false) as FrameLayout
        var recyclerView = rootView.findViewById<RecyclerView>(R.id.home_remotes_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = HomeRemoteListAdapter(MainActivity.remotePropList)
        }
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
class HomeRemoteListAdapter(private val propList: ArrayList<RemoteProperties>) : RecyclerView.Adapter<HomeRemoteListAdapter.MyViewHolder>(){

    class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.manage_remote_list_item, parent, false) as CardView

        return MyViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val prop = propList[position]
        setViewProps(holder.cardView,prop)
        holder.cardView.findViewById<TextView>(R.id.btn_count).text = prop.getButtons().length().toString()
        holder.cardView.setOnClickListener {
            RemoteDialog(holder.cardView.context,prop,RemoteDialog.MODE_VIEW_ONLY).show()
        }

    }

    private fun setViewProps(cardView: CardView, prop:RemoteProperties){
        cardView.findViewById<TextView>(R.id.vendor_name_text).text = prop.remoteVendor
        cardView.findViewById<TextView>(R.id.model_name_text).text = prop.remoteName
        cardView.findViewById<TextView>(R.id.remote_desc).text = prop.description
    }

    override fun getItemCount() = propList.size
}

