/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.healthconnect.testapps.toolbox.read

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.data.PermissionsRepository.TreeNode
import com.android.healthconnect.testapps.toolbox.read.theme.HealthFitnessGradleProjectTheme
import com.android.healthconnect.testapps.toolbox.viewmodels.PermissionsRequestViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.launch

@Composable
fun RequestPermissionsScreen(
    viewModel: PermissionsRequestViewModel =
        viewModel(factory = PermissionsRequestViewModel.Factory)
) {
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val allPermissionsAlreadyGrantedText =
        stringResource(R.string.all_permissions_already_granted_toast)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it
            ->
            scope.launch { snackBarHostState.showSnackbar(getPermissionsResultSnackBarText(it)) }
            viewModel.updateCounters(viewModel.tree)
        }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackBarHostState) }) {
        Surface {
            HealthFitnessGradleProjectTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PermissionRows(viewModel)
                    RequestButton(
                        name = stringResource(R.string.request),
                        onClick = {
                            val permissionsToGrant = viewModel.getRequestedPermissions()
                            if (permissionsToGrant.isEmpty()) {
                                scope.launch {
                                    snackBarHostState.showSnackbar(allPermissionsAlreadyGrantedText)
                                }
                            } else {
                                launcher.launch(permissionsToGrant.toTypedArray())
                            }
                        },
                        tree = viewModel.tree,
                    )
                    RequestButton(
                        name = stringResource(R.string.revoke),
                        onClick = {
                            val text = viewModel.revokePermissions()
                            scope.launch { snackBarHostState.showSnackbar(text) }
                        },
                        tree = viewModel.tree,
                    )
                    RequestButton(
                        name = stringResource(R.string.exit_process),
                        onClick = { exitProcess(status = 0) },
                    )
                }
            }
        }
    }
}

private fun getPermissionsResultSnackBarText(permissionMap: Map<String, Boolean>): String {
    val numberOfPermissionsGranted = permissionMap.values.count { it }
    val numberOfPermissionsDenied = permissionMap.keys.size - numberOfPermissionsGranted
    return "Granted: $numberOfPermissionsGranted Denied: $numberOfPermissionsDenied"
}

@Composable
fun RequestButton(name: String, onClick: () -> Unit, tree: TreeNode? = null) {
    val enabled = tree?.toggleableState != ToggleableState.Off
    Button(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(text = name)
    }
}

@Composable
fun PermissionRows(viewModel: PermissionsRequestViewModel) {
    Column { PermissionTree(viewModel, remember { viewModel.tree }) }
}

@Composable
fun PermissionTree(viewModel: PermissionsRequestViewModel, treeNode: TreeNode) {

    RequestRow(
        onOnlyButtonClick = {
            viewModel.tree.forEach { it.toggleableState = ToggleableState.Off }
            viewModel.onCheck(treeNode)
        },
        onCheck = { viewModel.onCheck(treeNode) },
        treeNode = treeNode,
    )

    for (child in treeNode.children) {
        PermissionTree(viewModel, treeNode = child)
    }
}

@Composable
private fun RequestRow(onOnlyButtonClick: () -> Unit, onCheck: () -> Unit, treeNode: TreeNode) {
    val isAllButton = treeNode.parent == null
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(treeNode.nameId),
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.padding(vertical = 12.dp, horizontal = 10.dp)
                    .weight(if (isAllButton) 6.5f else 4.5f),
        )

        if (!isAllButton) {
            Button(onClick = onOnlyButtonClick, modifier = Modifier.weight(2f)) {
                Text(maxLines = 1, text = stringResource(R.string.only))
            }
        }

        Text(
            text = treeNode.counter,
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.padding(vertical = 12.dp).weight(1.5f).wrapContentWidth(Alignment.End),
        )

        TriStateCheckbox(
            state = treeNode.toggleableState,
            onClick = onCheck,
            modifier = Modifier.weight(1f).wrapContentWidth(Alignment.End),
        )
    }
}
