import styled from 'styled-components';

export const Wrapper = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const Overlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  cursor: pointer;
`;

export const Modal = styled.div`
  position: relative;
  background-color: ${({ theme }) => theme.modal.backgroundColor};
  border-radius: 8px;
  max-width: 700px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 1001;
`;

export const Header = styled.h2`
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid ${({ theme }) => theme.modal.border};
  font-size: 20px;
  font-weight: 500;
`;

export const Form = styled.form`
  display: flex;
  flex-direction: column;
`;

export const Content = styled.div`
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const InfoSection = styled.div`
  padding: 12px;
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  border-radius: 4px;
  margin-bottom: 8px;

  p {
    margin: 0;
    font-size: 14px;
  }
`;

export const TextArea = styled.textarea`
  width: 100%;
  padding: 8px 12px;
  border: 1px solid ${({ theme }) => theme.input.borderColor.normal};
  border-radius: 4px;
  font-family: 'Monaco', monospace;
  font-size: 13px;
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  color: ${({ theme }) => theme.input.color.normal};
  resize: vertical;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.input.borderColor.active};
  }

  &::placeholder {
    color: ${({ theme }) => theme.input.color.placeholder};
  }
`;

export const CheckboxWrapper = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;

  input[type='checkbox'] {
    cursor: pointer;
  }

  label {
    cursor: pointer;
    font-size: 14px;
    user-select: none;
  }
`;

export const PermissionNote = styled.div`
  padding: 12px;
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  border-left: 3px solid ${({ theme }) => theme.button.primary.backgroundColor};
  border-radius: 4px;
  font-size: 13px;
  margin-top: 8px;

  strong {
    color: ${({ theme }) => theme.button.primary.backgroundColor};
  }
`;

export const Footer = styled.div`
  padding: 16px 24px;
  border-top: 1px solid ${({ theme }) => theme.modal.border};
  display: flex;
  justify-content: flex-end;
  gap: 12px;
`;
