package com.github.meudayhegde.esputils.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.meudayhegde.esputils.Strings
import com.github.meudayhegde.esputils.databinding.FragmentAboutBinding
import com.github.meudayhegde.esputils.listeners.OnFragmentInteractionListener
import java.io.InputStreamReader

class AboutFragment : androidx.fragment.app.Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    private var _binding: FragmentAboutBinding? = null
    private lateinit var fragmentBinding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            fragmentBinding = FragmentAboutBinding.inflate(inflater, container, false)
            _binding = fragmentBinding
            fragmentBinding.aboutApp.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml(
                    InputStreamReader(requireContext().assets.open("about.html")).readText(),
                    Html.FROM_HTML_MODE_LEGACY
                )
                else Html.fromHtml(InputStreamReader(requireContext().assets.open("about.html")).readText())
            fragmentBinding.aboutApp.movementMethod = LinkMovementMethod.getInstance()
            val onClickListener = View.OnClickListener {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW, Uri.parse(Strings.urlLicenseApache)
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
