package com.enesterzi.artbooknavigation.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.telecom.Call.Details
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.enesterzi.artbooknavigation.R
import com.enesterzi.artbooknavigation.databinding.FragmentUploadBinding
import com.enesterzi.artbooknavigation.model.Art
import com.enesterzi.artbooknavigation.roomdb.ArtDao
import com.enesterzi.artbooknavigation.roomdb.ArtDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream


class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedPicture : Uri? = null
    private var selectedBitmap : Bitmap? = null

    private lateinit var artDatabase: ArtDatabase
    private lateinit var artDao: ArtDao
    private val mDisposable = CompositeDisposable()
    var artFromMain : Art? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        artDatabase = Room.databaseBuilder(requireContext(), ArtDatabase::class.java, "Arts").build()

        artDao = artDatabase.artDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageView.setOnClickListener { selectImage(view) }
        binding.saveButton.setOnClickListener { save(view) }
        binding.deleteButton.setOnClickListener { delete(view) }

        arguments?.let {
            val info = UploadFragmentArgs.fromBundle(it).info
            if (info.equals("new")) {
                // New
                binding.artText.setText("")
                binding.artistText.setText("")
                binding.yearText.setText("")
                binding.saveButton.visibility = View.VISIBLE
                binding.deleteButton.visibility = View.GONE

                val selectedImageBackground = BitmapFactory.decodeResource(context?.resources, R.drawable.select_image)
                binding.imageView.setImageBitmap(selectedImageBackground)
            } else {
                // Old
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE

                val selectedId = UploadFragmentArgs.fromBundle(it).id
                mDisposable.add(
                    artDao.getArtById(selectedId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponseWithOldArt)
                )
            }
        }


    }

    private fun handleResponse() {
        val action = UploadFragmentDirections.actionUploadFragmentToListFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    private fun handleResponseWithOldArt(art: Art) {
        artFromMain = art
        binding.artText.setText(art.artName)
        binding.artistText.setText(art.artistName)
        binding.yearText.setText(art.year)

        art.image?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            binding.imageView.setImageBitmap(bitmap)
        }
    }

    private fun selectImage(view: View) {
        activity?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireActivity().applicationContext, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                {
                    // permission denied
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)
                    ) {
                        Snackbar.make(view,"Permission needed for gallery!", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Give Permission") {
                                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            }.show()
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                } else {
                    // permission granted
                    val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    // intent
                    resultLauncher.launch(intentToGallery)
                }
            } else {
                // Android 32- -> READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(requireActivity().applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // rationale
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Give Permission") {
                                // request permission
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            .show()
                    } else {
                        // request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                } else {
                    val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    // intent
                    resultLauncher.launch(intentToGallery)
                }
            }
        }
    }

    private fun registerLauncher () {
        // Choose the image
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedPicture = intentFromResult.data // URI
                    if (selectedPicture != null) {
                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedPicture!!)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            } else {
                                selectedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedPicture)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {result ->
            if (result) {
                // permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                resultLauncher.launch(intentToGallery)
            } else {
                //permission denied
                Toast.makeText(requireContext(), "Permisson needed!", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    fun makeSmallerBitmap(image: Bitmap, maximumSize : Int) : Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1) {
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)

    }

    fun save(view: View) {
        val artName = binding.artText.text.toString()
        val artistName = binding.artistText.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 250)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            val art = Art(artName,artistName, year, byteArray)

            mDisposable.add(
                artDao.insert(art)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    fun delete(view: View) {
        artFromMain?.let {
            mDisposable.add(artDao.delete(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}