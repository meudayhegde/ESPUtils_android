package com.github.meudayhegde.esputils.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.meudayhegde.esputils.R
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.FragmentAboutBinding
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener

class AboutFragment : androidx.fragment.app.Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private var _binding: FragmentAboutBinding? = null
    private lateinit var fragmentBinding: FragmentAboutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if(_binding == null ){
            fragmentBinding = FragmentAboutBinding.inflate(inflater, container, false)
            _binding = fragmentBinding
            fragmentBinding.aboutApp.text = TextUtils.join(" ",context?.resources?.getStringArray(R.array.about)?: arrayOf(" "))
            val onClickListener = View.OnClickListener {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Strings.urlLicenseApache)
                )
                startActivity(browserIntent)
            }
            fragmentBinding.fabLicense.setOnClickListener(onClickListener)
            fragmentBinding.pikoloLicense.setOnClickListener(onClickListener)
            fragmentBinding.materialLicense.setOnClickListener(onClickListener)
        }

        return fragmentBinding.root
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
