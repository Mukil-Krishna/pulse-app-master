package me.trevi.navparser.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import me.trevi.navparser.NavParserActivity
import me.trevi.navparser.NavigationDataModel
import me.trevi.navparser.activity.databinding.FragmentNavigationBinding
import me.trevi.navparser.lib.NavigationData
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class NavigationFragment : Fragment() {
    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!
    private val navDataModel: NavigationDataModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.stopNavButton.setOnClickListener {
            (activity as NavParserActivity).stopNavigation()
        }

        setNavigationData(NavigationData(true))
        navDataModel.liveData.observe(viewLifecycleOwner, { setNavigationData(it) })
    }

    override fun onStart() {
        super.onStart()
        (activity as NavParserActivity).hideMissingDataSnackbar()
    }

    override fun onStop() {
        (activity as NavParserActivity).stopServiceListener()
        super.onStop()
    }

    private fun setNavigationData(navData: NavigationData) {
        binding.navigationDest.text = getString(
            if (navData.isRerouting)
                R.string.recalculating_navigation
            else
                R.string.navigate_to
        ).format(navData.finalDirection)

        binding.nextDirection.text = navData.nextDirection.localeString
        binding.nextAction.text = navData.nextDirection.navigationDistance?.localeString

        binding.eta.text = getString(R.string.eta).format(
            if (navData.eta.time != null) {
                DateFormat.getTimeFormat(activity?.applicationContext).format(
                    Date.from(
                        LocalDateTime.of(LocalDate.now(), navData.eta.time).atZone(
                            ZoneId.systemDefault()
                        ).toInstant()
                    )
                )
            } else {
                navData.eta.localeString
            },
            navData.eta.duration?.localeString
        )

        binding.distance.text = getString(R.string.distance).format(navData.remainingDistance.localeString)
        binding.stopNavButton.isEnabled = navData.canStop
        binding.actionDirection.setImageBitmap(navData.actionIcon.bitmap)

        val bm = navData.actionIcon.bitmap
        if ( bm != null) {
            var baos = ByteArrayOutputStream()
            //bm is the bitmap object
            bm!!.compress(Bitmap.CompressFormat.PNG, 100, baos)
            var byteArray = baos.toByteArray()
            var md5 = MessageDigest.getInstance("MD5")
            val md5CheckSum = BigInteger(1, md5.digest(byteArray)).toString(16).padStart(32, '0').toString();
            binding.nextDirection.text = navData.nextDirection.localeString + ":"+ md5CheckSum;
            //md5CheckSum will be different for different image, hence you can use it to identify left, right, straight
            //Steps: 1) set the map which has left turn and right turn in your phone
            //2) checksum will be displayed in the screen in direction text, note that
            //3) then come here and use that check sum to figure out left, right, straight,
            // if ( md5CheckSum.equals("13e68aacc62531a385e2b3e9705e0701")) then straight;
            // etc
            // there are two different images for straight thus the or statement
            // if ( md5CheckSum.equals("7c2731606742cb8545a39dea66ba445e") || md5CheckSum.equals("b7e506e2329811258c5912fca19b3d8f")) then straight;
            // there are two types of right (slight right and right) thus the or statement
            // if ( md5CheckSum.equals("79c23f3c035d18baf29ab5078c387963") || md5CheckSum.equals("ca7fe0fa11d1e40a70df0fb4f69d3a92")) then right;
            // same thing for left
            // if ( md5CheckSum.equals("3f2e712ee53137133c147218bddfa5e4") || md5CheckSum.equals("1efb6b1cc749733fa6a5e7a21a597e55")) then left;
            // after the map takes the last turn it shows a symbol for destination but i believe this symbol can be ignored;
            // if ( md5CheckSum.equals("0fa51ebacc6511870edde8a6a481382e")) then destination;
        }

    }
}
