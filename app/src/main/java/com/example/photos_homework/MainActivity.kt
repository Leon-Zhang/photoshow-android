package com.example.photos_homework

//import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable

import androidx.navigation.compose.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.HttpURLConnection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.json.JSONArray
import java.net.URL
import javax.inject.Inject

// Data Model
data class Photo(val id: Int, val albumId: Int, val title: String, val url: String, val thumbnailUrl: String)

const val SEARCH_LABEL = "Search"
const val FAVORITES_BTN = "Favorites"
const val ALLPHOTOS_BTN = "All photos"
const val BACK_BTN = "Back"
const val IMAGE_CONTENTDESC = "Photo Image"
const val ERR_NOITEMSAVAIL = "No items available"
const val REMOVEFAV_BTN = "Remove from Favorites"
const val ADDFAV_BTN = "Add to Favorites"


@HiltViewModel
class PhotoViewModel @Inject constructor() : ViewModel() {

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos

    private val _favorites = MutableStateFlow<Set<Int>>(emptySet())
    val favorites: StateFlow<Set<Int>> = _favorites

    fun findPhotoById(photoId: Int): Photo? {
        return _photos.value.find { it.id == photoId }
    }

    fun fetchPhotos(){
        val url = URL("https://jsonplaceholder.typicode.com/photos")
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        val data = connection.inputStream.bufferedReader().readText()
        val jsonArray = JSONArray(data)

        val photoList = (0 until 20).map { i -> // Limit to 20 items
            val obj = jsonArray.getJSONObject(i)
            Photo(
                id = obj.getInt("id"),
                albumId = obj.getInt("albumId"),
                title = obj.getString("title"),
                url = obj.getString("url").replace("via.placeholder.com", "dummyimage.com"),
                thumbnailUrl = obj.getString("thumbnailUrl").replace("via.placeholder.com", "dummyimage.com")
            )
        }
        _photos.value = photoList
    }
    //Retrieve all photos from https end-point
    fun fetchPhotosAsync():Job {
        return viewModelScope.launch(Dispatchers.IO) {
            fetchPhotos()
        }
    }

    //Toggle favorites on/off by photo id
    fun toggleFavorite(photoId: Int) {
        //favorites = if (favorites.contains(photo.id)) favorites - photo.id else favorites + photo.id
        _favorites.value = _favorites.value.toMutableSet().apply {
            if (contains(photoId)) remove(photoId) else add(photoId)
        }
    }
}

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            //Global view model instance
            val viewModel = PhotoViewModel()
            val navController = rememberNavController()
            NavHost(navController, startDestination = "list") {
                composable("list") {
                    PhotoListScreen ({ photo ->
                        navController.navigate("detail/${photo.id}")
                    },viewModel)
                }
                composable("detail/{photoId}") { backStackEntry ->
                    val photoId = backStackEntry.arguments?.getString("photoId")?.toIntOrNull()
                    if (photoId!=null) {
                        val photo = viewModel.findPhotoById(photoId)
                        if (photo == null) {
                            val context = LocalContext.current
                            LaunchedEffect(Unit) {
                                Toast.makeText(context, "Photo not found ($photoId)", Toast.LENGTH_SHORT)
                                    .show()
                                navController.popBackStack() // Navigate back after showing message
                            }
                        } else {
                            //Navigate to photo detail screen
                            PhotoDetailScreen(photo, {
                                navController.popBackStack()
                            }, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoListScreen(onPhotoClick: (Photo) -> Unit, curViewModel: PhotoViewModel) {
    val viewModel: PhotoViewModel = curViewModel
    val photos by viewModel.photos.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val filteredPhotos = photos.filter { it.title.contains(searchQuery, ignoreCase = true) }
        .filter { !showFavorites || (it.id in favorites) }

    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.fetchPhotosAsync()
        }
    }

    Column {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(SEARCH_LABEL) },
            modifier = Modifier.fillMaxWidth()
        )
        Row (
            horizontalArrangement = Arrangement.Center
        ){
            Button(
                onClick = { showFavorites = false },
                colors = ButtonDefaults.buttonColors(
                    contentColor = if (!showFavorites) Color.Gray else Color.LightGray
                ),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(text = ALLPHOTOS_BTN)
            }

            Button(
                onClick = { showFavorites = true },
                colors = ButtonDefaults.buttonColors(
                    contentColor = if (showFavorites) Color.Gray else Color.LightGray
                ),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(text = FAVORITES_BTN)
            }
            /*RadioButton(selected  = (!showFavorites), onClick  = { showFavorites = false })
            Text(text ="All Photos",textAlign = TextAlign.Center )
            RadioButton(selected  = (showFavorites), onClick  = { showFavorites = true })
            Text(text ="Favorites",textAlign = TextAlign.Center)*/
        }
        //Show no item text if filtered photo collection is empty
        if (filteredPhotos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ERR_NOITEMSAVAIL,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn {
                items(filteredPhotos) { photo ->
                    PhotoItem(photo, isFavorite = favorites.contains(photo.id), {
                        viewModel.toggleFavorite(photo.id)

                    }, onClick = onPhotoClick)
                }
            }
        }
    }
}

//UI for display photo item on list screen
@Composable
fun PhotoItem(photo: Photo, isFavorite: Boolean, onFavoriteToggle: () -> Unit, onClick: (Photo) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clickable { onClick(photo) }) {
        Image(
            painter = rememberImagePainter(photo.thumbnailUrl.replace("via.placeholder.com", "dummyimage.com")),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
        Column(modifier = Modifier
            .weight(1f)
            .padding(8.dp)) {
            Text(photo.title)
        }
        IconButton(onClick = onFavoriteToggle) {
            val icon: Painter = painterResource(if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            Image(icon, contentDescription = "Favorite")
        }
    }
}

@Composable
fun PhotoDetailScreen(photo: Photo, onBack: () -> Unit, curViewModel: PhotoViewModel) {
    val viewModel: PhotoViewModel = curViewModel
    val isFavorite by viewModel.favorites.collectAsState()
    var showDialog by remember { mutableStateOf(false) }


    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onBack) {
            Text(BACK_BTN)
        }

        Image(
            painter = rememberImagePainter(photo.url.replace("via.placeholder.com", "dummyimage.com")),
            contentDescription = IMAGE_CONTENTDESC,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Text(photo.title, modifier = Modifier
            .fillMaxWidth(),style = MaterialTheme.typography.h6,textAlign = TextAlign.Center)

        Button(onClick = { showDialog=true }) {
            Text(if (photo.id in isFavorite) REMOVEFAV_BTN else ADDFAV_BTN)
        }

        if (showDialog) {
            ConfirmationDialog(
                isFavorite = (photo.id in isFavorite),
                onConfirm = {
                    viewModel.toggleFavorite(photo.id)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }

    }
}

@Composable
fun ConfirmationDialog(isFavorite: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFavorite) "Remove from Favorites?" else "Add to Favorites?") },
        text = { Text(if (isFavorite) "Are you sure you want to remove this from your favorites?" else "Do you want to add this to your favorites?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

/*class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}*/