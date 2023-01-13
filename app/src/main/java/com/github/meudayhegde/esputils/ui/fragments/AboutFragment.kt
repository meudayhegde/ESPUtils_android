package com.github.meudayhegde.esputils.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener

class AboutFragment : androidx.fragment.app.Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private var rootView: ScrollView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(rootView == null ){
            rootView = inflater.inflate(R.layout.fragment_about, container, false) as ScrollView
            rootView?.findViewById<TextView>(R.id.about_app)?.text = TextUtils.join(" ",context?.resources?.getStringArray(R.array.about)?: arrayOf(" "))
            val onClickListener = View.OnClickListener {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://www.apache.org/licenses/LICENSE-2.0")
                )
                startActivity(browserIntent)
            }
            val fabLicense = rootView!!.findViewById(R.id.fab_license) as LinearLayout
            fabLicense.setOnClickListener(onClickListener)
            val pikoloLicense = rootView!!.findViewById(R.id.pikolo_license) as LinearLayout
            pikoloLicense.setOnClickListener(onClickListener)
            val materialLicense = rootView!!.findViewById(R.id.material_license) as LinearLayout
            materialLicense.setOnClickListener(onClickListener)
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
