<template>
<div class="modal fade" id="createThemeModal" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
<div class="modal-dialog">
  <div class="modal-content">
    <div class="modal-header" style="text-align: center;">
      <h1 class="modal-title fs-5" id="exampleModalLabel">테마 추가</h1>
      <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
    </div>
    <div class="modal-body">
      <div class="text-style-custom">이모티콘</div>
      <div class="input-group-text d-flex justify-content-center" style="margin-left: 30px; margin-right: 30px;">
        <div v-if="emoticon">{{emoticon}}</div>
        <input type="text" class="form-control input-text" v-if="!emoticon" v-model="state.emoticon" maxlength="5" @input="checkEmoji">
      </div>
      <div v-if="state.emoticon && !state.emoticonCheck && !emoticon" class="danger-text">이모지만 입력해 주세요</div>
      <br>
      <div class="text-style-custom">테마제목</div>
      <div class="input-group-text d-flex justify-content-center" style="margin-left: 30px; margin-right: 30px;">
        <div v-if="themeName">{{themeName}}</div>
        <input type="text" class="form-control input-text" v-if="!themeName" v-model="state.name" maxlength="20">
      </div>
      <br>
      <div class="text-style-custom">공개 여부 설정</div>
      <div class="d-flex justify-content-around">
          <button @click="changeType(0)" class="white-add-button" id="Type0">공개</button>
          <button @click="changeType(1)" class="white-add-button" id="Type1">팔로우</button>
          <button @click="changeType(2)" class="white-add-button" id="Type2">비공개</button>
      </div>
      
    </div>
    <div class="modal-footer">
      <div class="d-flex justify-content-center" style="margin-top: 15px; margin-bottom: 15px;">
        <div v-if="emoticon">
          <!-- <button v-if="state.emoticonCheck" @click="createTheme()" class="white-add-button item" data-bs-dismiss="modal">테마 추가</button> -->
          <button @click="createTheme()" class="white-add-button" data-bs-dismiss="modal">테마 추가</button>
        </div>
        <div v-if="!emoticon">
          <button v-if="state.emoticon&&state.emoticonCheck&&state.type != -1" @click="registTheme()" class="white-add-button item" data-bs-dismiss="modal">테마 추가</button>
          <button v-else class="block-buttonn" data-bs-dismiss="modal">테마 추가</button>
        </div>
      </div>
    </div>
  </div>
</div>
</div>
</template>
  
<script lang="ts">
import { reactive } from '@vue/reactivity'
import { useStore } from "vuex";
import { computed } from '@vue/runtime-core';
export default {
  components: {
  },
  setup (){
    const store = useStore();
      const state = reactive({
        emoticon: store.getters.selectedThemeEmoticonForCreate,
        name: "",
        type: -1,
        emoticonCheck: false,
      })
    const emojiRegex = /([\u2700-\u27BF]|[\uE000-\uF8FF]|\uD83C[\uDC00-\uDFFF]|\uD83D[\uDC00-\uDFFF]|[\u2011-\u26FF]|\uD83E[\uDD10-\uDDFF])/g;
    const checkEmoji = () => {
      let check = false
      for (let alpha of state.emoticon) {
        const t = alpha.search(emojiRegex)
        if (t == -1) {
          state.emoticonCheck = false
          check = false
          break
        }
        else {check = true}
      }
      if (check) {
        state.emoticonCheck = true
      }
    }
    
    
    const createTheme = () => {
      store.dispatch('createUserTheme', { openType: state.type, challenge: false })
    }
    const registTheme = () => {
        store.dispatch('registTheme', { 
          openType: state.type,
          emoticon: state.emoticon,
          challenge: false,
        })
    }
    
    const isThemeIdx = computed(() => store.getters.isSelectedThemeIdxForCreate)
    const themeIdx = computed(() => store.getters.selectedThemeIdxForCreate)
    const themeName = computed(() => store.getters.selectedThemeNameForCreate)
    const emoticon = computed(()=>store.getters.selectedThemeEmoticonForCreate)
    const changeType = (_number:number)=>{
      state.type = _number
      for (let i = 0; i <= 2; i++){
        const btn = document.querySelector(`#Type${i}`)
        if (btn && i == _number) {
          btn.className = 'can-not-select-button'
        }
        else if(btn) {
          btn.className = 'white-add-button'
        }
      }
    }
    
    return { state, changeType, themeIdx, themeName, emoticon, isThemeIdx, registTheme, createTheme, checkEmoji }
  }
}
</script>
  
<style scoped lang="scss">
.text-style-custom{
  text-align: center;
  font-size: large;
  margin: 5px;
}
.input-text {
  text-align: center;
}

.danger-text {
  color: red;
  text-align: center;
}
.modal-dialog{
  margin-left: 10px;
  margin-right: 10px;
  margin-top: 10px;
}
</style>