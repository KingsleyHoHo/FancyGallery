package edu.vt.cs5254.fancygallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import edu.vt.cs5254.fancygallery.databinding.FragmentPhotoPageBinding

class PhotoPageFragment : Fragment() {

    private var _binding: FragmentPhotoPageBinding? = null
    private val binding: FragmentPhotoPageBinding
        get() = checkNotNull(_binding) { "FragmentPhotoPageBinding is NULL !!!" }

    private val args: PhotoPageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhotoPageBinding.inflate(inflater, container, false)
        binding.apply {
            webView.apply {
                webViewClient = WebViewClient()
                loadUrl(args.photoPageUri.toString())
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}