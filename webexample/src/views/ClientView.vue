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
const shopUrl = 'http://localhost:7002'
let isShopRunning = ref(false);
let appleSold = ref(0);
let testShop = setInterval(() => {
  if (nameServerStatus.value === 'started') {
    axios.get(shopUrl + '/testShop', {params: {nameIp: nameServerIp.value}})
        .then(function (response) {
          if (response.data.status === 0) {
            isShopRunning.value = true;
            clearInterval(testShop);
          }
        }).catch(() => {
    });
  }
}, 1000)

function syncSold() {
  axios.post(shopUrl + '/syncSoldApple', {num: 100})
      .catch(e => alert('出售失败' + e));
}

function asyncSold() {
  axios.post(shopUrl + '/asyncSoldApple', {num: 100})
      .catch(e => alert('出售失败' + e));
}

//苹果店服务器
const storeUrl = 'http://localhost:7003'
let isStoreRunning = ref(false);
let soldNum = ref(0);
//测试连接store
let testStore = setInterval(() => {
  if (nameServerStatus.value === 'started') {
    axios.get(storeUrl + '/testStore', {params: {nameIp: nameServerIp.value}})
        .then(function (response) {
          if (response.data.status === 0) {
            isStoreRunning.value = true;
            clearInterval(testStore);
          }
        }).catch(() => {
    });
  }
}, 1000);
//获取已出售数量
let getSoldNum = setInterval(() => {
  if (isStoreRunning.value) {
    axios.get(storeUrl + '/getSoldNum').then(response => {
      if (response.data.status === 0) {
        soldNum.value = response.data.object;
      }
    })
  }
}, 5000);

function changeSpeed(e) {
  axios.put(storeUrl + '/setSpeed', {speed: e.target.value})
      .then(() => {
        document.getElementById('input-consume-speed').placeholder = e.target.value;
      })
      .catch(() => alert('更新速度异常'));
}
</script>

<template>
  <div class="client">
    <div class="block1">
      <h2 class="green">苹果商店（Producer）<span style="color: green" v-show="isShopRunning">已连接</span></h2>
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

    <div class="block1">
      <h2 class="green">苹果服务器（Consumer）<span style="color: green" v-show="isStoreRunning">已连接</span></h2>
      <div class="set-ip">
        <label for="input-consume-speed">消费速度：</label>
        <input id="input-consume-speed" type="text" placeholder="10" @blur="changeSpeed"/>
        <span>个/秒</span>
      </div>
      <div>已出售{{ appleSold }}个苹果！</div>
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
