package com.github.kosher9.pdfgenerator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.net.toFile
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.github.kosher9.pdfgenerator.ui.theme.PdfGeneratorTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfGeneratorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    PdfGenerator()
                    PreviewPdf()
                }
            }
        }
    }

}

@Composable
fun PreviewPdf() {
    val file: File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "dia.pdf"
    )
    Column {
        PdfViewer(uri = file.toUri())
    }
}

@Composable
fun PdfViewer(
    modifier: Modifier = Modifier,
    uri: Uri,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
) {
    val decimalFormat = DecimalFormat("00")
    val rendererScope = rememberCoroutineScope()
    val mutex = remember { Mutex() }
    val renderer by produceState<PdfRenderer?>(null, uri) {
        rendererScope.launch(Dispatchers.IO) {
            try {
                val input =
                    ParcelFileDescriptor.open(uri.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
                value = PdfRenderer(input)
            } catch (e: Exception) {
                println(e)
            }

        }
        awaitDispose {
            val currentRenderer = value
            rendererScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    currentRenderer?.close()
                }
            }
        }
    }
    val state = rememberLazyListState()

    val visibleItemIds: List<Int> by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                emptyList()
            } else {
                visibleItemsInfo.map { it.index }
            }
        }
    }

    val context = LocalContext.current
    val imageLoader = LocalContext.current.imageLoader
    val imageLoadingScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
        val height = (width * sqrt(2f)).toInt()
        val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            state = state,
            verticalArrangement = verticalArrangement
        ) {
            items(
                count = pageCount,
                key = { index -> "$uri-$index" }
            ) { index ->
                Log.d("PdfRenderer", pageCount.toString())
                Log.d("PdfRenderer", visibleItemIds.toString())
                val cacheKey = MemoryCache.Key("$uri-$index")
                var bitmap by remember { mutableStateOf(imageLoader.memoryCache?.get(cacheKey) as? Bitmap?) }
                if (bitmap == null) {
                    DisposableEffect(uri, index) {
                        val job = imageLoadingScope.launch(Dispatchers.IO) {
                            val destinationBitmap =
                                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            mutex.withLock {
                                Log.d("PdfViewer", "Loading PDF $uri - page $index/$pageCount")
                                if (!coroutineContext.isActive) return@launch
                                try {
                                    renderer?.let {
                                        it.openPage(index).use { page ->
                                            page.render(
                                                destinationBitmap,
                                                null,
                                                null,
                                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    //Just catch and return in case the renderer is being closed
                                    return@launch
                                }
                            }
                            bitmap = destinationBitmap
                        }
                        onDispose {
                            job.cancel()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .aspectRatio(1f / sqrt(2f))
                            .fillMaxWidth()
                    )
                } else {
                    val request = ImageRequest.Builder(context)
                        .size(width, height)
                        .memoryCacheKey(cacheKey)
                        .data(bitmap)
                        .build()

                    Image(
                        modifier = Modifier
                            .background(Color.White)
//                            .aspectRatio(1f / sqrt(2f))
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentScale = ContentScale.Fit,
                        painter = rememberImagePainter(request),
                        contentDescription = "Page ${index + 1} of $pageCount"
                    )
                }
            }
        }
        BottomAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(60.dp)
                .align(Alignment.BottomCenter),
            contentColor = Color.Black,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.White)
                    .height(60.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (visibleItemIds.isNotEmpty()) {
                    Text(
                        text =
                        "${decimalFormat.format(visibleItemIds.last() + 1)} / ${
                            decimalFormat.format(pageCount)
                        } Pages",
                        modifier = Modifier
                            .background(
                                color = Color(0xFFF0F5F6),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(40.dp, 8.dp)
                    )
                }

            }
        }
    }
}

// on below line we are creating a
// pdf generator composable function for ui.
@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PdfGenerator() {

    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )
    )

    LaunchedEffect(key1 = Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }
    AsyncImage(
        model = imageUri,
        contentDescription = null,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxHeight()
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
    )

    // on below line we are creating a variable for
    // our context and activity and initializing it.
    val ctx = LocalContext.current
    val activity = (LocalContext.current as? Activity)

    Column(

        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .fillMaxSize()
            .padding(6.dp),

        verticalArrangement = Arrangement.Center,

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "PDF Generator",

            color = Color.Green,

            fontWeight = FontWeight.Bold,

            textAlign = TextAlign.Center,

            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()

                .padding(20.dp),

            onClick = {
                generatePDF(ctx)
            }) {

            Text(modifier = Modifier.padding(6.dp), text = "Generate PDF")
        }
    }

}

fun generatePDF(context: Context) {

    val pageHeight = 1120
    val pageWidth = 792

    lateinit var scaledbmp: Bitmap

    val pdfDocument: PdfDocument = PdfDocument()

    val paint: Paint = Paint()
    val title: Paint = Paint()

    val images = listOf(
        BitmapFactory.decodeResource(context.resources, R.drawable.screenshot),
        BitmapFactory.decodeResource(context.resources, R.drawable.screenshot),
        BitmapFactory.decodeResource(context.resources, R.drawable.screenshot)
    )
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.screenshot_1)

//    val images: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.screenshot)
    Log.d("PdfRenderer", "Generating PDF")
    for (i in 1..10) {
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Draw the image on the canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Finish the page
        pdfDocument.finishPage(page)
    }
    Log.d("PdfRenderer", "PDF generated")


    val file: File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "dia.pdf"
    )

    try {
        pdfDocument.writeTo(FileOutputStream(file))
//        writeTextData(file, "Life of pdf")

        Toast.makeText(context, "PDF file generated..", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()

        Toast.makeText(context, "Fail to generate PDF file..", Toast.LENGTH_SHORT)
            .show()
    }
    pdfDocument.close()
}

private fun writeTextData(file: File, data: String) {
    var fileOutputStream: FileOutputStream? = null
    try {
        fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(data.toByteArray())
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PdfGeneratorTheme {
        Greeting("Android")
    }
}