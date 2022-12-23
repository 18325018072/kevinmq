<script setup>
import {ref} from "vue";
import axios from "axios";

// NameServer数据
let nameServerIp = ref("http://localhost:700");
let nameServerStatus = ref('none');

//NameServer方法
function testNameServer() {
  nameServerStatus.value = 'none';
  axios.get(nameServerIp.value + '/testNameServer')
      .then(function (response) {
        if (response.data.status === 0) {
          nameServerStatus.value = 'connected';
          if (response.data.info === 'running') {
            nameServerStatus.value = 'started';
          }
        }
      }).catch(() => {
  });
}

//苹果店前端
let appleSold = ref(0);

function syncSold() {

}

function asyncSold() {

}

//苹果店服务器
let appleOut = ref(0);
</script>

<template>
  <div class="client">
    <div class="block1">
      <h2 class="green">苹果商店（Producer）</h2>
      <div class="set-ip">
        <label for="input-name-ip">NameServer 的 IP 地址：</label>
        <input id="input-name-ip" type="text" v-model="nameServerIp" placeholder="url地址" @change="testNameServer"/>
        <span class="run-tip" v-if="nameServerStatus==='started'">已启动</span>
        <span class="available-tip" v-else-if="nameServerStatus==='connected'">已连接</span>
        <span class="down-tip" v-else>未连接</span>
      </div>
      <div v-if="nameServerStatus==='started'">
        <span>无情地卖苹果：</span>
        <button @click="syncSold">同步卖1000个</button>
        <button @click="asyncSold">异步卖1000个</button>
      </div>
    </div>
  </div>
</template>

<style>
.block1 {
  border: solid;
  margin: 1rem;
  padding: 0.5rem;
}
</style>
