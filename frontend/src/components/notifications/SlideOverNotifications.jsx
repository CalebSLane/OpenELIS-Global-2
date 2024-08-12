import { Renew, NotificationFilled, Email, Filter } from "@carbon/icons-react";
import { formatTimestamp } from "../utils/Utils";
import Spinner from "../common/Sprinner";
import { FormattedMessage, useIntl } from "react-intl";

export default function SlideOverNotifications(props) {
  const intl = useIntl();

  const {
    loading,
    notifications,
    showRead,
    markNotificationAsRead,
    setShowRead,
    getNotifications,
    markAllNotificationsAsRead,
  } = props;

  const NotificationButton = ({ icon, label, onClick, disabled }) => (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: disabled ? "#f0f0f0" : "white",
        padding: "0.5rem 0.8rem",
        fontWeight: "600",
        border: "none",
        borderRadius: "0.3rem",
        transition: "background-color 0.2s ease-in-out",
        color: disabled ? "#a1a1a1" : "#837994",
        whiteSpace: "nowrap",
        cursor: disabled ? "not-allowed" : "pointer",
      }}
      onMouseEnter={(e) => {
        if (!disabled) e.currentTarget.style.backgroundColor = "#DFDAE8";
      }}
      onMouseLeave={(e) => {
        if (!disabled) e.currentTarget.style.backgroundColor = "white";
      }}
    >
      {icon}
      <span style={{ fontSize: "0.75rem", marginLeft: "0.5rem" }}>{label}</span>
    </button>
  );

  return (
    <div
      style={{
        backgroundColor: "white",
        borderRadius: "0.3rem",
        transition: "background-color 0.2s ease-in-out",
        padding: "1rem",
        maxWidth: "600px",
        margin: "0 auto",
      }}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            flexWrap: "wrap",
          }}
        >
          {[
            {
              icon: <Renew />,
              label: intl.formatMessage({
                id: "notification.slideover.button.reload",
              }),
              onClick: () => {
                getNotifications();
              },
            },
            {
              icon: <NotificationFilled />,
              label: intl.formatMessage({
                id: "notification.slideover.button.subscribe",
              }),
              onClick: () => {},
            },
            {
              icon: <Email />,
              label: intl.formatMessage({
                id: "notification.slideover.button.markallasread",
              }),
              onClick: () => {
                markAllNotificationsAsRead();
              },
            },
            {
              icon: <Filter />,
              label: showRead
                ? intl.formatMessage({
                    id: "notification.slideover.button.hideread",
                  })
                : intl.formatMessage({
                    id: "notification.slideover.button.showread",
                  }),
              onClick: () => setShowRead(!showRead),
            },
          ].map(({ icon, label, onClick }, index) => (
            <NotificationButton
              key={index}
              icon={icon}
              label={label}
              onClick={onClick}
            />
          ))}
        </div>
      </div>
      <div>
        {loading ? (
          <div style={{ textAlign: "center", marginTop: "1rem" }}>
            <Spinner />
          </div>
        ) : notifications && notifications.length > 0 ? (
          notifications.map((notification, index) => (
            <div
              key={index}
              style={{
                position: "relative",
                marginTop: "0.5rem",
                cursor: "pointer",
                borderRadius: "0.5rem",
                padding: "1.5rem 1rem",
                transition: "all 0.2s ease-in-out",
                backgroundColor: notification.readAt ? "#f3f3f3" : "white",
              }}
              onMouseOver={(e) => {
                if (!notification.readAt)
                  e.currentTarget.style.backgroundColor = "#f3f3f3";
              }}
              onMouseOut={(e) => {
                if (!notification.readAt)
                  e.currentTarget.style.backgroundColor = "white";
              }}
            >
              <div style={{ fontWeight: "500" }}>{notification.message}</div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginTop: "0.5rem",
                  color: "#4b5563",
                  fontSize: "0.75rem",
                }}
              >
                <div>{formatTimestamp(notification.createdDate)}</div>
                <NotificationButton
                  icon={<Email />}
                  label={intl.formatMessage({
                    id: "notification.slideover.button.markasread",
                  })}
                  onClick={() => markNotificationAsRead(notification.id)}
                  disabled={!!notification.readAt}
                />
              </div>
            </div>
          ))
        ) : (
          <div
            style={{
              textAlign: "center",
              padding: "2rem",
              color: "#4b5563",
            }}
          >
            <svg
              width="100"
              height="102"
              viewBox="0 0 100 102"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <g opacity="0.45">
                <path
                  opacity="0.45"
                  d="M49.4233 71.0879L6.7079 30.1421"
                  stroke="#C8C6C4"
                  strokeWidth="0.6524"
                  strokeMiterlimit="10"
                ></path>
                <path
                  opacity="0.45"
                  d="M0.950762 29.1425C2.17888 30.3344 8.31955 31.68 8.31955 31.68C8.31955 31.68 6.47714 25.3748 5.21065 24.1829C3.94415 22.9911 2.02544 23.1448 0.835701 24.5289C-0.31566 25.913 -0.277356 27.9891 0.950762 29.1425Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M11.1219 29.5656C11.1603 31.6033 14.7679 37.8701 14.7679 37.8701C14.7679 37.8701 18.0684 31.0649 18.03 29.0657C17.9917 27.028 16.3798 25.5286 14.4609 25.6439C12.5803 25.7977 11.0835 27.5279 11.1219 29.5656Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M6.59228 34.8325C8.51121 34.6787 14.7669 37.87 14.7669 37.87C14.7669 37.87 8.6645 42.0221 6.74557 42.1374C4.82663 42.2912 3.25318 40.7534 3.17642 38.7157C3.13805 36.7549 4.67334 34.9863 6.59228 34.8325Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M19.065 36.1781C19.1034 38.6387 23.5168 46.2127 23.5168 46.2127C23.5168 46.2127 27.5464 37.9852 27.4697 35.5246C27.4313 33.064 25.4743 31.2185 23.1716 31.4107C20.8689 31.603 19.0266 33.7175 19.065 36.1781Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M13.5779 42.5606C15.8807 42.3684 23.4796 46.2515 23.4796 46.2515C23.4796 46.2515 16.1109 51.2496 13.7698 51.4419C11.4671 51.6341 9.50973 49.7886 9.47135 47.328C9.43297 44.8674 11.2752 42.7529 13.5779 42.5606Z"
                  fill="#C8C6C4"
                ></path>
              </g>
              <g opacity="0.45">
                <path
                  opacity="0.45"
                  d="M65.5811 97.4627L93.2902 78.5083"
                  stroke="#C8C6C4"
                  strokeWidth="0.3695"
                  strokeMiterlimit="10"
                ></path>
                <path
                  opacity="0.45"
                  d="M96.6297 78.508C95.8238 79.0463 92.293 79.2385 92.293 79.2385C92.293 79.2385 93.9432 75.8938 94.7108 75.3555C95.5168 74.8172 96.5913 75.0864 97.1286 75.9322C97.6659 76.778 97.3973 77.9698 96.6297 78.508Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M90.9115 77.7776C90.6812 78.8926 88.0713 82.0838 88.0713 82.0838C88.0713 82.0838 86.8817 77.9699 87.0736 76.855C87.3039 75.74 88.3401 75.0095 89.3763 75.2786C90.4509 75.5477 91.1034 76.6627 90.9115 77.7776Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M92.9444 81.1606C91.8698 80.8914 88.0702 82.1217 88.0702 82.1217C88.0702 82.1217 91.0636 85.0053 92.1382 85.236C93.2128 85.4666 94.2106 84.7746 94.4409 83.6596C94.6712 82.5446 93.9806 81.4297 92.9444 81.1606Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M85.8065 80.7374C85.5379 82.1215 82.3911 85.9277 82.3911 85.9277C82.3911 85.9277 80.9325 80.9681 81.2011 79.584C81.4698 78.1999 82.6982 77.3541 84.003 77.6617C85.2695 78.0462 86.0752 79.3918 85.8065 80.7374Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M88.3016 84.8131C87.0351 84.5055 82.4297 85.9665 82.4297 85.9665C82.4297 85.9665 86.0756 89.4268 87.3421 89.7728C88.6086 90.0804 89.8754 89.2346 90.1441 87.8505C90.3744 86.5049 89.5681 85.1206 88.3016 84.8131Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M82.9274 89.1196C81.3539 88.7351 75.7509 90.5422 75.7509 90.5422C75.7509 90.5422 80.2026 94.7713 81.7378 95.1558C83.3113 95.5403 84.8082 94.5022 85.1537 92.849C85.4607 91.1573 84.4625 89.5041 82.9274 89.1196Z"
                  fill="#C8C6C4"
                ></path>
                <path
                  opacity="0.45"
                  d="M79.3591 84.0827C79.2823 85.659 75.8668 90.3879 75.8668 90.3879C75.8668 90.3879 73.0651 84.967 73.1803 83.3907C73.257 81.8144 74.7154 80.6994 76.4425 80.8917C78.1311 81.0455 79.4359 82.5064 79.3591 84.0827Z"
                  fill="#C8C6C4"
                ></path>
              </g>
              <path
                d="M57.5594 22.9141C63.9818 22.9141 69.1881 17.7847 69.1881 11.4571C69.1881 5.12946 63.9818 0 57.5594 0C51.137 0 45.9307 5.12946 45.9307 11.4571C45.9307 17.7847 51.137 22.9141 57.5594 22.9141Z"
                fill="#FFE5B9"
              ></path>
              <path
                d="M26.0126 98.4241C31.5391 100.385 37.6797 98.0779 39.944 92.7338C41.2489 89.6197 40.9035 86.2748 39.3683 83.5451C42.4002 82.6993 45.0485 80.5463 46.3533 77.4321C48.5793 72.088 45.8929 66.1671 40.5583 63.7065C39.1767 63.0914 37.8332 62.5916 36.4899 62.1686L39.8675 70.1657C40.213 70.9346 39.8291 71.8572 39.0231 72.1648C38.2172 72.4724 37.2962 72.1265 36.9891 71.3191L32.7289 61.246C30.2726 60.8615 27.8934 60.7845 25.6674 60.9768L26.7802 63.5528C27.1256 64.3217 26.7417 65.2445 25.9358 65.5521C25.1298 65.8597 24.2088 65.5136 23.9018 64.7062L22.4435 61.3612C20.2175 61.7841 18.145 62.3993 16.226 63.0913L17.838 66.8977C18.1834 67.6667 17.7996 68.5893 16.9936 68.8968C16.1877 69.2044 15.2667 68.8585 14.9596 68.0511L13.3476 64.2832C8.24328 66.7054 5.13483 69.512 5.13483 69.512C5.13483 69.512 5.3651 73.6643 7.24566 78.9315L9.04915 78.201C9.85511 77.8935 10.7765 78.2394 11.0835 79.0467C11.3906 79.8541 11.0451 80.7385 10.2391 81.0461L8.35841 81.815C9.20274 83.622 10.239 85.5058 11.5055 87.3513L17.0322 85.083C17.8382 84.7754 18.7592 85.1215 19.0662 85.9289C19.4116 86.6978 19.0278 87.6204 18.2218 87.928L13.4627 89.8888C14.9211 91.5805 16.6484 93.1569 18.6824 94.5794L27.4327 91.0422C28.2387 90.7346 29.1597 91.0807 29.4667 91.8881C29.7737 92.6955 29.4282 93.5797 28.6223 93.8872L21.9829 96.5785C23.2493 97.3475 24.5926 97.9243 26.0126 98.4241Z"
                fill="#F2F2F2"
              ></path>
              <path
                d="M95.4006 65.0133L87.9936 66.0129C87.1109 66.1283 86.2666 65.5131 86.1515 64.6288C86.0364 63.7445 86.6503 62.9373 87.533 62.8219L97.2813 61.5148C98.3559 59.208 99.0468 56.8627 99.469 54.5943L94.1342 55.2863C93.2515 55.4017 92.4072 54.7865 92.2921 53.9022C92.177 53.0179 92.7909 52.2106 93.6736 52.0953L99.891 51.2879C100.083 48.9811 100.006 46.7126 99.7759 44.675L97.7033 44.9442C96.8206 45.0595 95.9764 44.4443 95.8612 43.56C95.7461 42.6758 96.3604 41.8683 97.2431 41.753L99.2771 41.484C98.1641 35.7554 96.13 31.9107 96.13 31.9107C96.13 31.9107 91.8317 31.0264 85.9214 31.4878L86.4971 35.7169C86.6122 36.6012 85.9979 37.4085 85.1152 37.5238C84.2325 37.6392 83.3882 37.0242 83.2731 36.1399L82.6974 31.8723C80.625 32.2183 78.399 32.718 76.173 33.5254L76.6719 37.2933C76.787 38.1776 76.1731 38.9848 75.2904 39.1002C74.4077 39.2155 73.5634 38.6003 73.4483 37.7161L73.0645 34.7943C70.9537 35.7939 68.8428 37.101 66.8471 38.7157L68.3822 49.9807C68.4974 50.865 67.8831 51.6723 67.0004 51.7876C66.1177 51.903 65.2734 51.2878 65.1583 50.4035L63.9301 41.4455C62.9322 42.4835 62.0114 43.6369 61.0903 44.9057C57.5978 49.8653 58.327 56.5935 63.2011 60.2459C66.0411 62.3605 69.5335 62.9372 72.719 62.1298C72.7573 65.3593 74.2543 68.5505 77.0943 70.665C81.9684 74.3175 88.7229 73.241 92.6759 68.5889C93.7505 67.4355 94.633 66.2051 95.4006 65.0133Z"
                fill="#F2F2F2"
              ></path>
              <path
                d="M82.2453 24.552C83.5502 18.9048 72.0534 11.4157 56.5666 7.82457C41.0797 4.23344 27.4674 5.90015 26.1625 11.5474C24.8577 17.1946 36.3545 24.6836 51.8413 28.2748C67.3281 31.8659 80.9405 30.1992 82.2453 24.552Z"
                fill="#FFD590"
              ></path>
              <path
                d="M80.8154 63.2452C69.4937 43.0991 71.2976 31.6804 68.3041 22.0687C63.5068 6.57463 44.1254 6.57466 39.1745 22.4916C36.2194 31.988 38.0232 44.6369 27.469 64.7446C25.243 69.0507 83.9241 68.7816 80.8154 63.2452Z"
                fill="#595A5A"
              ></path>
              <path
                d="M84.1547 74.4332C84.2698 88.0433 70.5302 95.9635 54.1425 96.0788C37.7932 96.1942 24.975 88.4663 24.8982 74.8561C24.7831 61.246 37.985 50.1348 54.3344 50.0195C70.7221 49.9041 84.0779 60.823 84.1547 74.4332Z"
                fill="#595A5A"
              ></path>
              <path
                d="M54.5656 50.2112C58.1732 50.2112 61.5505 49.5577 64.3522 48.4811C61.5505 47.2508 58.2882 46.5205 54.7957 46.5205C51.2265 46.5205 47.8878 47.2894 45.0477 48.5581C47.811 49.6346 51.0731 50.2112 54.5656 50.2112Z"
                fill="#BF8F68"
              ></path>
              <path
                d="M64.0841 47.7509C63.8922 47.7125 60.8987 47.2512 60.5149 46.9052C59.8625 46.4054 59.5938 45.6748 59.6705 44.6752L59.7856 36.7168H49.577V44.5599C49.5003 45.4826 49.3468 46.1747 49.0782 46.5207C48.8095 46.8667 47.044 47.6741 45.3554 47.6741C47.85 51.4419 59.1332 52.7105 64.0841 47.7509Z"
                fill="url(#paint0_radial)"
              ></path>
              <path
                d="M54.6045 42.0995C60.2639 42.0995 64.8517 35.8855 64.8517 28.2201C64.8517 20.5548 60.2639 14.3408 54.6045 14.3408C48.9452 14.3408 44.3574 20.5548 44.3574 28.2201C44.3574 35.8855 48.9452 42.0995 54.6045 42.0995Z"
                fill="#BD8E67"
              ></path>
              <path
                d="M54.355 25.7979H47.5233V37.9854H54.355V25.7979Z"
                fill="url(#paint1_radial)"
              ></path>
              <path
                d="M53.5679 34.5249C54.1052 34.794 54.374 34.9479 54.374 34.9863H54.4123C55.909 33.7176 55.9859 31.526 54.374 28.9116L54.1435 29.8345C53.8749 30.565 53.4145 31.1801 52.7621 31.7568C52.455 31.9875 52.3016 32.3334 52.2632 32.7563C52.2248 33.1792 52.34 33.5253 52.5319 33.7944C52.7238 34.0251 53.0689 34.2942 53.5679 34.5249Z"
                fill="#BD8E67"
              ></path>
              <path
                d="M57.2141 37.2556C56.9071 37.64 56.5617 37.9476 56.1396 38.1782C55.7174 38.4089 55.2952 38.6011 54.7963 38.7165C54.2974 38.8318 53.7601 38.7933 53.2612 38.678C52.7623 38.5626 52.3019 38.2936 51.9565 37.9475C51.7646 37.7553 51.7646 37.4478 51.9565 37.294C52.0717 37.1787 52.225 37.1401 52.3785 37.1401H52.4554C52.8392 37.1786 53.1846 37.2556 53.4917 37.2556C53.8371 37.2556 54.144 37.2555 54.4894 37.1786C54.8348 37.1017 55.1803 37.0249 55.5257 36.9096C55.8711 36.7942 56.2548 36.6405 56.6002 36.5251L56.677 36.4866C56.9073 36.4097 57.2142 36.525 57.2909 36.7556C57.3293 36.9479 57.2909 37.1402 57.2141 37.2556Z"
                fill="#886345"
              ></path>
              <path
                d="M49.8071 30.1039C50.337 30.1039 50.7666 29.3981 50.7666 28.5275C50.7666 27.657 50.337 26.9512 49.8071 26.9512C49.2772 26.9512 48.8477 27.657 48.8477 28.5275C48.8477 29.3981 49.2772 30.1039 49.8071 30.1039Z"
                fill="#3A312E"
              ></path>
              <path
                d="M59.0571 30.1429C59.587 30.1429 60.0165 29.4372 60.0165 28.5666C60.0165 27.696 59.587 26.9902 59.0571 26.9902C58.5272 26.9902 58.0976 27.696 58.0976 28.5666C58.0976 29.4372 58.5272 30.1429 59.0571 30.1429Z"
                fill="#3A312E"
              ></path>
              <path
                d="M73.6016 35.3714C73.6016 35.3714 72.4504 32.6417 71.9131 31.3345C71.6445 30.604 70.6466 28.9123 70.1861 28.1818C69.7639 27.5666 69.4568 27.2207 69.2649 27.1054C68.9578 26.9516 68.6125 26.9899 68.1903 27.1821C67.9984 27.0284 67.7682 26.8361 67.4996 26.6054C67.3077 26.4517 67.1158 26.3749 66.9239 26.3364C66.5401 26.4518 66.4248 26.6824 66.5784 27.1054C66.5784 27.1054 68.4207 33.7566 68.2672 34.3333L71.2989 37.755"
                fill="#BD8E67"
              ></path>
              <path
                d="M94.0189 55.1712C94.0189 55.1327 93.9807 55.1327 93.9807 55.0942C93.9807 55.0558 93.9421 55.0557 93.9421 55.0173C93.7886 54.7097 93.5968 54.4022 93.3282 54.1715C93.1747 54.0177 75.3668 36.7936 73.6014 35.3711L71.2987 37.7547C71.2987 37.7547 76.0577 44.2137 80.3945 50.0577C80.5864 50.3268 80.3561 50.7112 80.0107 50.6344C73.4479 49.2503 66.7699 47.9432 65.2731 47.8278C62.0493 47.5587 63.9302 49.4811 63.4696 52.2877C63.0091 54.979 64.3522 57.5548 67.1538 58.3622C69.6868 59.0927 82.0063 60.2077 90.1042 60.9382C92.2151 61.1304 94.1341 59.6309 94.4027 57.5548C94.5562 56.7089 94.4027 55.8632 94.0189 55.1712Z"
                fill="#C3C3C3"
              ></path>
              <path
                d="M58.1358 98.8849C65.1207 100.346 71.6068 99.8845 76.4425 99.0386C75.6749 96.578 73.6409 91.0802 71.9139 85.7745C70.5706 81.6222 70.8008 72.6257 71.2613 69.8576C71.9138 65.7053 73.3336 61.8221 73.3336 59.6307C73.3336 52.4411 65.0438 46.6357 54.7967 46.6357C44.5496 46.6357 36.2599 52.4411 36.2599 59.6307C36.2599 61.899 36.7589 65.2824 37.8719 69.6268C38.9849 73.9713 39.8676 81.8146 38.3325 85.7362C35.7995 92.1953 34.3411 95.7324 33.5352 98.2314C33.5352 98.3852 33.535 98.5005 33.5734 98.6159C40.0978 97.77 49.6924 97.1163 58.1358 98.8849Z"
                fill="#C3C3C3"
              ></path>
              <path
                d="M15.2274 55.1712C15.2274 55.1327 15.2656 55.1327 15.2656 55.0942C15.2656 55.0558 15.3042 55.0557 15.3042 55.0173C15.4577 54.7097 15.6494 54.4022 15.9181 54.1715C16.0716 54.0177 33.8795 36.7936 35.6449 35.3711L37.9476 37.7547C37.9476 37.7547 33.1886 44.2137 28.8518 50.0577C28.6599 50.3268 28.8902 50.7112 29.2356 50.6344C35.7983 49.2503 42.476 47.9432 43.9728 47.8278C47.1966 47.5587 45.3161 49.4811 45.7767 52.2877C46.2372 54.979 44.8941 57.5548 42.0924 58.3622C39.5595 59.0927 27.24 60.2077 19.1421 60.9382C17.0312 61.1304 15.1122 59.6309 14.8436 57.5548C14.7284 56.7089 14.8819 55.8632 15.2274 55.1712Z"
                fill="#C3C3C3"
              ></path>
              <path
                d="M64.3149 50.2115C64.6987 49.6733 65.313 48.4816 65.5817 47.8664C62.2044 47.3666 58.635 46.6744 54.4901 46.5591C50.4987 46.6744 46.7374 47.482 43.7823 47.8664C43.9358 48.4816 44.3583 49.5964 44.6653 50.0578C45.6248 51.4034 46.8145 52.0956 48.1194 52.0956H61.1681C62.3195 52.0956 63.3554 51.4803 64.3149 50.2115Z"
                fill="#BD8E67"
              ></path>
              <path
                d="M44.5094 32.2183C44.5094 32.2183 42.2066 29.2963 40.019 28.6812L38.5607 33.6792L43.3962 34.6404"
                fill="#BD8E67"
              ></path>
              <path
                d="M64.7743 31.7567C64.7743 31.7567 67.0771 28.8349 69.2647 28.2197L70.723 33.2178L65.8874 34.179"
                fill="#BD8E67"
              ></path>
              <path
                d="M49.3848 12.6876C49.3848 12.6876 50.3442 17.4933 55.8323 19.6463C63.7767 22.7989 63.5849 27.4126 63.5849 27.4126C63.5849 27.4126 67.9984 21.9916 62.6254 14.8789C57.0989 7.53558 49.3848 12.6876 49.3848 12.6876Z"
                fill="#595A5A"
              ></path>
              <path
                d="M35.6456 35.3714C35.6456 35.3714 36.7968 32.6417 37.3341 31.3345C37.6027 30.604 38.6005 28.9123 39.0611 28.1818C39.4833 27.5666 39.7904 27.2207 39.9823 27.1054C40.2894 26.9516 40.6347 26.9899 41.0569 27.1821C41.2488 27.0284 41.479 26.8361 41.7476 26.6054C41.9395 26.4517 42.1314 26.3749 42.3233 26.3364C42.7071 26.4518 42.8224 26.6824 42.6688 27.1054C42.6688 27.1054 40.8265 33.7566 40.98 34.3333L37.9483 37.755"
                fill="#BD8E67"
              ></path>
              <path
                opacity="0.35"
                d="M46.1223 24.6826C45.7385 24.6826 45.3932 24.7212 45.0477 24.7981L50.881 30.5266C50.9578 30.1806 50.9964 29.8345 50.9964 29.4885C51.0348 26.8356 48.8472 24.6826 46.1223 24.6826Z"
                fill="white"
              ></path>
              <path
                d="M66.1946 33.2949C65.6957 33.9485 63.892 34.41 63.585 34.6023C63.3163 34.8329 63.2395 35.0252 63.3162 35.179C63.4314 35.3712 63.6616 35.4096 64.007 35.3712C65.7724 35.0636 67.1926 35.5249 68.0753 35.3327C68.8429 35.1405 71.6826 33.7563 72.1816 33.4872"
                fill="#BD8E67"
              ></path>
              <path
                d="M41.5176 33.4487C42.0165 34.1023 45.3554 35.0249 45.6624 35.2556C45.9311 35.4863 46.0079 35.6785 45.9311 35.8323C45.816 36.0246 45.5858 36.063 45.2404 36.0246C43.475 35.717 42.0547 36.1783 41.172 35.9861C40.4045 35.7938 37.5644 34.4098 37.0654 34.1407"
                fill="#BD8E67"
              ></path>
              <path
                d="M25.6671 99.9999C25.6671 99.9999 43.5899 95.8092 58.0971 98.8465C72.6042 101.884 85.0004 96.6167 85.0004 96.6167"
                stroke="#C0C0C0"
                strokeWidth="3.2"
                strokeMiterlimit="10"
                strokeLinecap="round"
                strokeLinejoin="round"
              ></path>
              <path
                d="M53.1458 28.5283C53.2993 31.9501 49.1927 35.641 45.777 35.7947C42.3613 35.9485 39.598 31.7964 39.4445 28.4131C39.291 24.9913 43.2823 22.7998 46.6596 22.6461C50.0753 22.4538 52.9923 25.1066 53.1458 28.5283Z"
                fill="#3E8EED"
              ></path>
              <path
                d="M44.7785 35.8712C48.191 35.8712 50.9573 33.0998 50.9573 29.6812C50.9573 26.2626 48.191 23.4912 44.7785 23.4912C41.3659 23.4912 38.5993 26.2626 38.5993 29.6812C38.5993 33.0998 41.3659 35.8712 44.7785 35.8712Z"
                fill="#72ACF1"
              ></path>
              <path
                d="M44.7793 34.4483C47.4076 34.4483 49.538 32.3139 49.538 29.6809C49.538 27.048 47.4076 24.9136 44.7793 24.9136C42.151 24.9136 40.0202 27.048 40.0202 29.6809C40.0202 32.3139 42.151 34.4483 44.7793 34.4483Z"
                fill="#3E8EED"
              ></path>
              <path
                d="M44.7786 33.18C46.7074 33.18 48.2709 31.6136 48.2709 29.6813C48.2709 27.7491 46.7074 26.1826 44.7786 26.1826C42.8498 26.1826 41.2859 27.7491 41.2859 29.6813C41.2859 31.6136 42.8498 33.18 44.7786 33.18Z"
                fill="#A7CBF6"
              ></path>
              <path
                d="M54.2961 28.4512C54.4496 31.873 58.8631 35.1793 62.2788 35.0255C65.6945 34.8717 68.0742 30.4889 67.8823 27.1056C67.7288 23.6838 63.5453 21.8768 60.1679 22.0306C56.7522 22.1459 54.1426 25.0295 54.2961 28.4512Z"
                fill="#3E8EED"
              ></path>
              <path
                d="M62.7003 35.0638C66.1128 35.0638 68.8791 32.2925 68.8791 28.8739C68.8791 25.4553 66.1128 22.6841 62.7003 22.6841C59.2877 22.6841 56.5211 25.4553 56.5211 28.8739C56.5211 32.2925 59.2877 35.0638 62.7003 35.0638Z"
                fill="#72ACF1"
              ></path>
              <path
                d="M62.7022 33.6414C65.3305 33.6414 67.4609 31.5068 67.4609 28.8738C67.4609 26.2408 65.3305 24.1064 62.7022 24.1064C60.0739 24.1064 57.9431 26.2408 57.9431 28.8738C57.9431 31.5068 60.0739 33.6414 62.7022 33.6414Z"
                fill="#3E8EED"
              ></path>
              <path
                d="M62.7012 32.3727C64.6301 32.3727 66.1935 30.8063 66.1935 28.874C66.1935 26.9417 64.6301 25.3755 62.7012 25.3755C60.7724 25.3755 59.2085 26.9417 59.2085 28.874C59.2085 30.8063 60.7724 32.3727 62.7012 32.3727Z"
                fill="#A7CBF6"
              ></path>
              <path
                opacity="0.45"
                d="M65.8103 27.2977L62.6249 32.2958C61.3968 32.2958 60.2838 31.6036 59.6697 30.604L63.0469 25.3369C64.2367 25.4523 65.273 26.2212 65.8103 27.2977Z"
                fill="white"
              ></path>
              <path
                opacity="0.45"
                d="M47.9636 28.1815L44.7782 33.1796C43.5501 33.1796 42.4371 32.4876 41.823 31.488L45.2003 26.2207C46.39 26.336 47.4263 27.105 47.9636 28.1815Z"
                fill="white"
              ></path>
              <path
                d="M53.4909 28.2204C54.9746 28.2204 56.1774 27.0155 56.1774 25.5292C56.1774 24.0428 54.9746 22.8379 53.4909 22.8379C52.0072 22.8379 50.8044 24.0428 50.8044 25.5292C50.8044 27.0155 52.0072 28.2204 53.4909 28.2204Z"
                fill="#72ACF1"
              ></path>
              <path
                d="M51.9179 26.029C51.6876 26.029 51.5342 25.8752 51.4959 25.6445C51.4959 25.4138 51.6494 25.2215 51.8797 25.2215L55.065 25.0293C55.2953 25.0293 55.487 25.1831 55.487 25.4138C55.487 25.6444 55.3335 25.8367 55.1032 25.8367L51.9179 26.029Z"
                fill="#3E8EED"
              ></path>
              <defs>
                <radialGradient
                  id="paint0_radial"
                  cx="0"
                  cy="0"
                  r="1"
                  gradientUnits="userSpaceOnUse"
                  gradientTransform="translate(55.7637 33.0607) scale(12.1229 11.9404)"
                >
                  <stop stopColor="#886345"></stop>
                  <stop offset="0.9974" stopColor="#BD8E67"></stop>
                </radialGradient>
                <radialGradient
                  id="paint1_radial"
                  cx="0"
                  cy="0"
                  r="1"
                  gradientUnits="userSpaceOnUse"
                  gradientTransform="translate(53.9697 32.8024) scale(4.33929 4.23852)"
                >
                  <stop stopColor="#886345"></stop>
                  <stop offset="0.9373" stopColor="#BD8E67"></stop>
                </radialGradient>
              </defs>
            </svg>

            <h4 style={{ fontSize: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage id="notification.slideover.empty.header" />
            </h4>
            <p style={{ fontSize: "0.875rem" }}>
              <FormattedMessage id="notification.sliderover.empty.message" />
            </p>
          </div>
        )}
      </div>
    </div>
  );
}