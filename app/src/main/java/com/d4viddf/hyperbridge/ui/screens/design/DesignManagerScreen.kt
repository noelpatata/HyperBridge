package com.d4viddf.hyperbridge.ui.screens.design

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.models.translator.isDesign
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.components.island.rememberIslandTemplateSample
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.screens.translators.TranslatorCardItem
import com.d4viddf.hyperbridge.ui.screens.translators.TranslatorViewModel
import kotlinx.coroutines.launch

/**
 * Every design in one place, like the Translators and Themes managers. The list shows each design
 * either as its live island preview or as the translator-style icon row; the choice is remembered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignManagerScreen(
    onBack: () -> Unit,
    onEditDesign: (id: String) -> Unit,
    viewModel: TranslatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allTranslators by viewModel.allTranslators.collectAsState()
    val showPreviews by preferences.designListShowPreviewFlow.collectAsState(initial = true)
    val designs = remember(allTranslators) { allTranslators.filter { it.isDesign } }

    var showAddDesign by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<CustomTranslator?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importTranslator(uri) { result ->
            val msg = if (result.isSuccess) {
                context.getString(R.string.translators_import_success, result.getOrNull()?.meta?.name ?: "")
            } else {
                context.getString(R.string.translators_import_failed, result.exceptionOrNull()?.localizedMessage ?: "")
            }
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val design = pendingExport
        pendingExport = null
        if (uri == null || design == null) return@rememberLauncherForActivityResult
        viewModel.exportTranslatorToUri(design, uri) { result ->
            val msg = context.getString(
                if (result.isSuccess) R.string.translators_export_success else R.string.translators_export_failed
            )
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.design_manager_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.design_manager_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { scope.launch { preferences.setDesignListShowPreview(!showPreviews) } },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = if (showPreviews) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.ViewAgenda,
                            contentDescription = stringResource(
                                if (showPreviews) R.string.design_manager_show_icons else R.string.design_manager_show_previews
                            )
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("*/*", "application/zip", "application/octet-stream", "application/json"))
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.translators_import_button))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDesign = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.design_add_design_title))
            }
        }
    ) { padding ->
        if (designs.isEmpty()) {
            DesignManagerEmptyState(
                onAddDesign = { showAddDesign = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(designs, key = { _, item -> item.id }) { index, design ->
                    val shape = getExpressiveShape(designs.size, index, ShapeStyle.Large)
                    val onToggle = { enabled: Boolean -> viewModel.toggleTranslator(design.id, enabled) }
                    val onShare = {
                        viewModel.shareTranslator(context, design) { result ->
                            if (result.isFailure) {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.translators_share_failed)) }
                            }
                        }
                    }
                    val onExport = {
                        pendingExport = design
                        exportLauncher.launch("${design.meta.name.ifBlank { design.id }}.htrans")
                    }

                    if (showPreviews) {
                        DesignPreviewItem(
                            design = design,
                            shape = shape,
                            onToggle = onToggle,
                            onClick = { onEditDesign(design.id) },
                            onDuplicate = { viewModel.duplicateTranslator(design) },
                            onDelete = { viewModel.deleteTranslator(design.id) },
                            onShare = onShare,
                            onExport = onExport
                        )
                    } else {
                        TranslatorCardItem(
                            translator = design,
                            shape = shape,
                            onToggle = onToggle,
                            onClick = { onEditDesign(design.id) },
                            onDuplicate = { viewModel.duplicateTranslator(design) },
                            onDelete = { viewModel.deleteTranslator(design.id) },
                            onShare = onShare,
                            onExport = onExport
                        )
                    }
                }
            }
        }
    }

    if (showAddDesign) {
        AddDesignFlow(
            onDismiss = { showAddDesign = false },
            onDesignCreated = { design ->
                showAddDesign = false
                viewModel.saveTranslator(design)
                Toast.makeText(context, R.string.design_design_created, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun DesignPreviewItem(
    design: CustomTranslator,
    shape: Shape,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    val template = IslandTemplateCatalog.find(design.presentation.templateId)

    Surface(
        onClick = onClick,
        shape = shape,
        color = if (design.isEnabled) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = design.meta.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = template?.let { stringResource(it.nameRes) }
                            ?: stringResource(R.string.translator_pres_mode_widget),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = design.isEnabled, onCheckedChange = onToggle)
            }

            if (template != null) {
                HyperOsIslandPreview(
                    translator = design,
                    sample = rememberIslandTemplateSample(template),
                    showChrome = false
                )
            } else {
                HyperOsIslandPreview(translator = design, showChrome = false)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
            ) {
                DesignActionButton(Icons.Default.Share, R.string.translators_share_button, onShare)
                DesignActionButton(Icons.Default.FileDownload, R.string.translators_export_button, onExport)
                DesignActionButton(Icons.Default.ContentCopy, R.string.translators_action_duplicate, onDuplicate)
                DesignActionButton(Icons.Default.Edit, R.string.translators_action_edit, onClick)
                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.translators_action_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DesignActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Icon(icon, contentDescription = stringResource(labelRes), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun DesignManagerEmptyState(onAddDesign: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.DashboardCustomize,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = stringResource(R.string.design_designs_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.design_designs_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Button(onClick = onAddDesign, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.design_add_design_title))
            }
        }
    }
}
