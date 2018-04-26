package io.mytc.sood

import io.mytc.sood.vm.state.Memory

trait Function extends ((Memory) => Memory)
